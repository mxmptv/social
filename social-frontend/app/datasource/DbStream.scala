package datasource

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import com.crawler.core.runners.{CrawlerAgent, CrawlerClient, CrawlerConfig, CrawlerMaster}
import com.crawler.dao.KafkaUniqueSaverInfo
import com.crawler.osn.common.TaskDataResponse
import com.crawler.osn.instagram.InstagramNewGeoPostsSearchTask
import com.crawler.util.Util
import com.mongodb.BasicDBObject
import com.mongodb.util.JSON
import org.apache.kafka.common.serialization.StringDeserializer
import redis.clients.jedis.{JedisPool, JedisPoolConfig}

import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.concurrent.duration._


/**
  * Created by vipmax on 03.07.17.
  */
object DbStream {
  val topicsToUsers = mutable.Map[String, mutable.HashSet[ActorRef]]()

  val pool = new JedisPool(new JedisPoolConfig(), "localhost")
  val jedis = pool.getResource

  val key = "topics"

  /** remove all topics first */
//  jedis.smembers(key).foreach(topic => jedis.srem(key, topic))

  def addTopic(topic: String, userChannel:ActorRef) = synchronized {
//    if(!topicsToUsers.containsKey(topic)) {
//      jedis.sadd(key, topic)
//    }

    val users = topicsToUsers.getOrElse(topic, mutable.HashSet())
    users += userChannel
    topicsToUsers(topic) = users
  }

  def removeTopic(topic: String, userChannel:ActorRef) = synchronized {
    val users = topicsToUsers(topic)
    users -= userChannel
//    if (users.isEmpty) {
//      topicsToUsers -= topic
//      jedis.srem(key, topic)
//    }
  }

  implicit val actorSystem = ActorSystem()
  implicit var materializer = ActorMaterializer()

  val stringDeserializer = new StringDeserializer

  var consumerSettings = ConsumerSettings(actorSystem, stringDeserializer, stringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("social")
    .withMaxWakeups(10)

  def handleMessage(msg: CommittableMessage[String, String]) = {
    val dbo = JSON.parse(msg.record.value()).asInstanceOf[BasicDBObject]
    val data = DbUtil.convert(dbo)
    println(data)

    val topic = data.getString("topic")
    synchronized {
      topicsToUsers(topic).foreach(u => u ! data.toJson)
    }
  }

  implicit val name = "SocialApp"


  class SocialApp extends CrawlerClient {
    override def afterBalancerWakeUp() {
      context.system.scheduler.schedule(
        0 seconds, 10 seconds, self, "instagram"
      )(context.dispatcher)
    }

    override def receiveMassage(massage: Any): Unit = massage match {
      case "instagram" =>
       topicsToUsers.keys.foreach { topic =>
         val task = InstagramNewGeoPostsSearchTask(
           query = topic,
           saverInfo = KafkaUniqueSaverInfo("localhost:9092", "localhost", "posts")
         )
         send(task)
       }
    }
    override def handleTaskDataResponse(tr: TaskDataResponse) = tr match {
      case any: TaskDataResponse =>
        println(s"any response = ${any.getClass.getSimpleName}")
    }
  }

  new Thread(new Runnable {
    override def run() = {
      startKafkaStream()
      startCrawlerClient()
    }
  }).start()

  private def startKafkaStream() {
    Consumer.committableSource(consumerSettings, Subscriptions.topics("posts"))
      .runForeach(handleMessage)(materializer)
  }

  private def startCrawlerClient() {
    // TODO: only for local debug mode
    CrawlerMaster.main(Array())
    CrawlerAgent.main(Array())

    Thread.sleep(5000)

    val masterIp = Util.getCurrentIp() // TODO: add parameter to setttings file
    val myIp = Util.getCurrentIp()

    val clusterName = "crawler"
    val system = ActorSystem(clusterName,
      CrawlerConfig.getConfig(clusterName, masterIp, myIp, name)
    )
    system.actorOf(Props[SocialApp], name)
    system.whenTerminated
  }
}
