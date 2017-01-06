import akka.actor.ActorRef

abstract class Message

case class Initialize(nodeId: Int, nextNode: ActorRef, totalNodes: Int) extends Message

case class Ping(numPing: Int) extends Message

case class Pong(numPong: Int) extends Message

case object LeaveCS extends Message

case class PerformCS(nodeId: Int) extends Message