import akka.actor.{Actor, ActorRef, Props}

import scala.util.Random

/**
  * Created by mike on 05.01.17.
  */
class Node extends Actor {
  var nextNode: ActorRef = null
  var numNodes: Int = -1
  var totalNodes: Int = -1 // TODO: do the token thing modulo with this
  var nodeId: Int = -1
  val worker = context.actorOf(Props[Worker])

  def randomSleep(n: Double = 4) = {
    val sleepFor = n * (scala.util.Random.nextInt(500) + 250)
    Thread.sleep(sleepFor.toInt)
  }

  def sendPing(v: Int) = {
    if (v < 0) throw new RuntimeException("Attempted to send negative ping")
    if (Random.nextInt(15) == 0) {
      println(s"${Console.MAGENTA_B}${Console.WHITE}Node $nodeId: Ping lost!${Console.RESET}")
    } else {
      randomSleep(1)
      println(s"Node $nodeId: sending PING = $v")
      nextNode ! Ping(v)
    }
  }

  def sendPong(v: Int) = {
    if (v > 0) throw new RuntimeException("Attempted to send positive pong")
    if (Random.nextInt(15) == 0) {
      println(s"${Console.BLUE_B}${Console.WHITE}Node $nodeId: Pong lost!${Console.RESET}")
    } else {
      randomSleep(2)
      println(s"Node $nodeId: sending PONG = $v")
      nextNode ! Pong(v)
    }
  }

  def discardIfObsolete(num: Int, m: Int) = {
    if (math.abs(num) < math.abs(m)) {
      println(s"Node $nodeId: received obsolete token = $num")
      true
    } else false
  }

  def receive = {
    case Initialize(id, nodes) => {
      val next = (nodes ::: List(nodes.head))(id + 1)
      println(s"Initializing id=$id next=$next total=$nodes.size")
      nodeId = id
      nextNode = next

      // Start the fire if nodeId is 0
      if (nodeId == 0) {
        nextNode ! Ping(1)
        nextNode ! Pong(-1)
      }

      context.become(noToken(0))
    }

    case _ => throw new RuntimeException("Send an Initialize message before performing actions")
  }

  def noToken(m: Int): Receive = {
    case Ping(rcvPingNo) => {
      println(s"[$nodeId] m=$m rcv=$rcvPingNo")
      if (!discardIfObsolete(rcvPingNo, m)) {
        println(s"[$nodeId] noToken rx ping=$rcvPingNo m=$m")
        worker ! PerformCS(nodeId, m)
        context.become(hasPing(rcvPingNo))
      }
    }
    case Pong(rcvPongNo) => {
      println(s"[$nodeId] m=$m rcv=$rcvPongNo")
      if (!discardIfObsolete(rcvPongNo, m)) {
        println(s"[$nodeId] noToken rx pong=$rcvPongNo m=$m")
        if (rcvPongNo == m) { // regenerate ping if lost
          context.become(hasBoth(rcvPongNo - 1)) // we got only pong but as we regenerate the ping it's ours
          worker ! PerformCS(nodeId, m)
        } else { // normal situation
          context.become(noToken(rcvPongNo))
          sendPong(rcvPongNo)
        }
      }
    }
  }

  def hasPing(m: Int): Receive = {
    case Ping(rcvPingNo) => {
      println(s"[$nodeId] m=$m rcv=$rcvPingNo")
      if (!discardIfObsolete(rcvPingNo, m)) {
        // This should never happen
        println(s"${Console.RED}[$nodeId] hasPing rx ping=$rcvPingNo m=$m${Console.RESET}")
      }
    }
    case Pong(rcvPongNo) => {
      println(s"[$nodeId] m=$m rcv=$rcvPongNo")
      if (!discardIfObsolete(rcvPongNo, m)) {
        println(s"[$nodeId] hasPing rx pong=$rcvPongNo m=$m")
        context.become(hasBoth(rcvPongNo))
      }
    }
    case LeaveCS(prevM) => {
      println(s"[$nodeId] hasPing rx leave m=$m")
      if (m == prevM) { // regenerate pong if lost
        context.become(noToken(-(m+1)))
        sendPing(m+1)
        sendPong(-(m+1))
      } else { // normal situation
        context.become(noToken(m))
        sendPing(m)
      }
    }
  }

  def hasBoth(m: Int): Receive = {
    case Ping(rcvPingNo) => {
      println(s"[$nodeId] m=$m rcv=$rcvPingNo")
      if (!discardIfObsolete(rcvPingNo, m)) {
        // this should never happen
        println(s"${Console.RED}[$nodeId] hasBoth rx ping=$rcvPingNo m=$m${Console.RESET}")
      }
    }
    case Pong(rcvPongNo) => {
      println(s"[$nodeId] m=$m rcv=$rcvPongNo")
      if (!discardIfObsolete(rcvPongNo, m)) {
        // this should never happen
        println(s"${Console.RED}[$nodeId] hasBoth rx pong=$rcvPongNo m=$m${Console.RESET}")
      }
    }
    case LeaveCS(_) => {
      // if we have both tokens, for sure the last one was a pong
      // and it is assumed the pingNo and pongNo have already been updated
      sendPing(-(m-1))
      sendPong(m-1)
      context.become(noToken(m-1))
    }
  }
}
