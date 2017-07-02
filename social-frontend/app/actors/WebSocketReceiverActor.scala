package actors

import java.util

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import com.mongodb.BasicDBObject
import com.mongodb.util.JSON
import org.apache.kafka.common.serialization.StringDeserializer
import play.api.Logger
import redis.clients.jedis.{JedisPool, JedisPoolConfig}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.duration._





object WebSocketReceiverActor {
  def apply(outappendChannel: ActorRef): WebSocketReceiverActor = new WebSocketReceiverActor(outappendChannel)
  def props(outputChannel: ActorRef) = Props(WebSocketReceiverActor(outputChannel))
}

class WebSocketReceiverActor(outputChannel: ActorRef) extends Actor {
  val userTopics = mutable.Set("sea")

  override def preStart() {
    Logger.info("New web-socket connection created")
    userTopics.foreach(t => datasource.DbStream.addTopic(t, outputChannel))
  }

    override def postStop(){
      Logger.info("Web-socket connection closed")
      userTopics.foreach(t => datasource.DbStream.removeTopic(t, outputChannel))
    }

    def receive = {
      case message: String =>
        Logger.info(s"Received message: $message")
        message match {
          case t if t.startsWith("addTopic:") =>
            val newTopic = t.substring("addTopic:".length)
            userTopics += newTopic
            datasource.DbStream.addTopic(newTopic, outputChannel)

          case t if t.startsWith("removeTopic:") =>
            val newTopic = t.substring("removeTopic:".length)
            userTopics -= newTopic
            datasource.DbStream.removeTopic(newTopic, outputChannel)
        }
    }
}

