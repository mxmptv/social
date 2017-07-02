package datasource

import akka.actor.{ActorRef, ActorSystem}
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import com.mongodb.BasicDBObject
import com.mongodb.util.JSON
import org.apache.kafka.common.serialization.StringDeserializer
import redis.clients.jedis.{JedisPool, JedisPoolConfig}

import scala.collection.mutable
import scala.collection.JavaConversions._

/**
  * Created by vipmax on 03.07.17.
  */
object DbStream {
  val topicsToUsers = mutable.Map[String, mutable.HashSet[ActorRef]]()


  val pool = new JedisPool(new JedisPoolConfig(), "localhost")
  val jedis = pool.getResource

  val key = "topics"

  /** remove all topics first */
  jedis.smembers(key).foreach(topic => jedis.srem(key, topic))

  def addTopic(topic: String, userChannel:ActorRef) = synchronized {
    if(!topicsToUsers.containsKey(topic)) {
      jedis.sadd(key, topic)
    }

    val users = topicsToUsers.getOrElse(topic, mutable.HashSet())
    users += userChannel
    topicsToUsers(topic) = users
  }

  def removeTopic(topic: String, userChannel:ActorRef) = synchronized {
    val users = topicsToUsers(topic)
    users -= userChannel
    if (users.isEmpty) {
      topicsToUsers -= topic
      jedis.srem(key, topic)
    }
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

  new Thread(new Runnable {
    override def run() = {
      Consumer.committableSource(consumerSettings, Subscriptions.topics("posts"))
        .runForeach (handleMessage)(materializer)
    }
  }).start()

}
