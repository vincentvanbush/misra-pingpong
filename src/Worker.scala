import akka.actor.Actor

import scala.util.Random

/**
  * Created by mike on 05.01.17.
  */
class Worker extends Actor {
  def receive = {
    case PerformCS(nodeId, prevM) => {
      println(s"${Console.BLUE}[WORKER] Node $nodeId entering CS${Console.RESET}")
      Thread.sleep(Random.nextInt(1900) + 1000)
      println(s"${Console.YELLOW}[WORKER] Node $nodeId leaving CS${Console.RESET}")

      sender ! LeaveCS(prevM)
    }

    case _ => throw new RuntimeException("Unrecognized message received by worker")
  }
}
