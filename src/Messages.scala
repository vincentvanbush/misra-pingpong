import akka.actor.ActorRef

abstract class Message

case class Initialize(nodeId: Int, nodes: List[ActorRef]) extends Message

case class Ping(numPing: Int) extends Message

case class Pong(numPong: Int) extends Message

case class LeaveCS(prevM: Int) extends Message

case class PerformCS(nodeId: Int, prevM: Int) extends Message