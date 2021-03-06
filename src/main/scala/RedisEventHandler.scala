import RedisClientActor.SubscriptionState
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.redis._

object RedisEventHandler {
  case class Subscribe(channels: Seq[String])
  case class Unsubscribe(channels: Seq[String])
}

class RedisClientActor(redisClient: RedisClient) extends Actor with ActorLogging {
  import RedisEventHandler._
  var state = SubscriptionState(subscriber = Map.empty, subscribed = Map.empty)

  override def receive: Receive = {
    // from other actor
    case Subscribe(channels) =>
      state = state.onSubscribe(sender, channels: _*)
      val me = self
      redisClient.subscribe(channels.head, channels.tail: _*) {
        msg: PubSubMessage =>
          // from com.redis.PubSub.Consumer thread
          me ! msg
      }

    case Unsubscribe(channels) =>
      val oldState = state
      state = state.onUnsubscribe(sender, channels: _*)
      val toUnsubscribe = channels.filter(
        c => oldState.numSubscriber(c) > 0 && state.numSubscriber(c) == 0
      )
      if (toUnsubscribe.nonEmpty) {
        redisClient.unsubscribe(toUnsubscribe.head, toUnsubscribe.tail: _*)
      }

    // from com.redis
    case S(channel, count) =>
      log.info("subscribed to {} / subscribing to {}", channel, count)

    case U(channel, count) =>
      log.info("unsubscribed to {} / subscribing to {}", channel, count)

    case M(channel, data) =>
      log.info("message has arrived from redis.")

    // NOT handling com.redis.redisclient.E
  }
}

// This manages subscribers' state. It can be used optionally. 
object RedisClientActor {
  def props(redisClient: RedisClient) =
    Props(new RedisClientActor(redisClient))

  case class SubscriptionState(subscriber: Map[String, Set[ActorRef]],
                               subscribed: Map[ActorRef, Set[String]]) {
    def onSubscribe(actor: ActorRef, channels: String*): SubscriptionState = {

      val newSubscribed = subscribed.updated(
        actor,
        subscribed.getOrElse(actor, Set.empty) ++ channels
      )
      val newSubscriber = channels.foldLeft(subscriber) { (s, channel) =>
        s.updated(channel, s.getOrElse(channel, Set.empty) + actor)
      }

      SubscriptionState(subscriber = newSubscriber, subscribed = newSubscribed)
    }

    def onUnsubscribe(actor: ActorRef, channels: String*): SubscriptionState = {

      val newSubscribed = subscribed.updated(
        actor,
        subscribed.getOrElse(actor, Set.empty) -- channels
      )
      val newSubscriber = channels.foldLeft(subscriber) { (s, channel) =>
        s.updated(channel, s.getOrElse(channel, Set.empty) - actor)
      }
      SubscriptionState(subscriber = newSubscriber, subscribed = newSubscribed)
    }

    def numSubscriber(channel: String): Int = {
      subscriber.getOrElse(channel, Set.empty).size
    }
  }

}
