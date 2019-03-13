
import RedisSubscriber.{Publish, Subscribe}
import akka.actor._

import com.redis.RedisClient
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext

object TestDriver {

  implicit val system = ActorSystem("test", ConfigFactory.load())
  implicit val executionContext: ExecutionContext = system.dispatcher

  def main(args: Array[String]): Unit = {
    var redisIp = "127.0.0.1"
    var redisPort = 6379
    val s = new RedisClient(redisIp, redisPort)

    val testActor = system.actorOf(Props(new RedisSubscriberActor(s)))

    // subscribe channel 'test'
    testActor ! Subscribe(Seq("test"))

    system.scheduler.scheduleOnce(Duration.create(3, TimeUnit.MILLISECONDS)) {
      testActor ! Publish("test", "hello world")
    }
  }
}
