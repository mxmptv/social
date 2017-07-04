package controllers

import actors.WebSocketReceiverActor
import akka.actor.ActorRef
import play.api.Play.current
import play.api.mvc._

class ReactiveApplication /*@Inject()(ws: WSClient)*/ extends Controller {

  def index = Action {

    Ok(views.html.main(getCurrentIp()))
  }

  def scroll = Action {
    Ok(views.html.scroll())
  }

  def reactive = WebSocket.acceptWithActor[String, String] {
    request => WebSocketReceiverActor.props
  }

  def addTag(tag: String) = Action  {
    println("tag = " + tag)
    Ok("added")
  }

  def getCurrentIp(): String = {
    import java.net.NetworkInterface
    import collection.JavaConversions._

    NetworkInterface.getNetworkInterfaces.foreach{ ee =>
      ee.getInetAddresses.foreach { i =>
        if (i.getHostAddress.startsWith("192.168"))
          return i.getHostAddress
      }
    }

    return "127.0.0.1"
  }

}
