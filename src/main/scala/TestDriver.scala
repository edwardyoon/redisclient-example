
import RedisSubscriber.{Published, Subscribe}
import akka.actor._
import com.redis.{E, M, PubSubMessage, RedisClient, S, U}

import scala.concurrent.ExecutionContext
import com.redis.RedisClient
import com.typesafe.config.ConfigFactory

import scala.util.parsing.json._


object TestDriver {

  implicit val system = ActorSystem("heimdallr", ConfigFactory.load())

  def main(args: Array[String]): Unit = {
    var redisIp = "127.0.0.1"
    var redisPort = 6379
    val s = new RedisClient(redisIp, redisPort)

    val testActor = system.actorOf(Props(new RedisSubscriberActor(s)))

    testActor ! Subscribe(Seq("test"))


    testActor ! Published("test", "hello world")
  }
}
