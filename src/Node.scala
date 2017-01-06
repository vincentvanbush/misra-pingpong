import akka.actor.{Actor, ActorRef, Props}

/**
  * Created by mike on 05.01.17.
  */
class Node extends Actor {
  var m = 0
  var pingNo = 0
  var pongNo = 0
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
    randomSleep(1)
    println(s"Node $nodeId: sending PING = $v")
    nextNode ! Ping(v)
  }

  def sendPong(v: Int) = {
    if (v > 0) throw new RuntimeException("Attempted to send positive pong")
    randomSleep(0.5)
    println(s"Node $nodeId: sending PONG = $v")
    nextNode ! Pong(v)
  }

  def discardIfObsolete(num: Int) = {
    if (math.abs(num) < math.abs(m)) {
      println(s"Node $nodeId: received obsolete token = $num")
      true
    } else false
  }

  def receive = {
    case Initialize(id, next, total) => {
      println(s"Initializing id=$id next=$next total=$total")
      nodeId = id
      nextNode = next

      // Start the fire if nodeId is 0
      if (nodeId == 0) {
        nextNode ! Ping(1)
        nextNode ! Pong(-1)
      }

      context.become(noToken)
    }

    case _ => throw new RuntimeException("Send an Initialize message before performing actions")
  }

  def noToken: Receive = {
    case Ping(numPing) => {
      if (!discardIfObsolete(numPing)) {
        println(s"[$nodeId] noToken rx ping=$numPing m=$m")
        context.become(hasPing)
        pingNo = numPing
        worker ! PerformCS(nodeId)
      }
    }
    case Pong(numPong) => {
      if (!discardIfObsolete(numPong)) {
        println(s"[$nodeId] noToken rx pong=$numPong m=$m")
        pongNo = numPong
        if (pongNo == m) { // regenerate ping if lost
          pongNo = numPong - 1
          pingNo = -pongNo
          context.become(hasBoth) // we got only pong but as we regenerate the ping it's ours
          worker ! PerformCS(nodeId)
          m = pongNo
        } else { // normal situation
          sendPong(pongNo)
        }
      }
    }
  }

  def hasPing: Receive = {
    case Ping(numPing) => {
      if (!discardIfObsolete(numPing)) {
        println(s"${Console.RED}[$nodeId] hasPing rx ping=$numPing m=$m${Console.RESET}")
      }
    }
    case Pong(numPong) => {
      if (!discardIfObsolete(numPong)) {
        println(s"[$nodeId] hasPing rx pong=$numPong m=$m")
        context.become(hasBoth)
        m = numPong
        pongNo = numPong - 1
        pingNo = -pongNo + 1
      }
    }
    case LeaveCS => {
      println(s"[$nodeId] hasPing rx leave m=$m")
      if (pingNo == m) { // regenerate pong if lost
        pingNo += 1
        pongNo -= 1
        sendPing(pingNo)
        sendPong(pongNo)
      } else { // normal situation
        m = pingNo
        sendPing(pingNo)
      }
      context.become(noToken)
    }
  }

  def hasBoth: Receive = {
    case Ping(numPing) => {
      if (!discardIfObsolete(numPing)) {
        println(s"${Console.RED}[$nodeId] hasBoth rx ping=$numPing m=$m${Console.RESET}")

      }
    }
    case Pong(numPong) => {
      if (!discardIfObsolete(numPong)) {
        println(s"${Console.RED}[$nodeId] hasBoth rx pong=$numPong m=$m${Console.RESET}")

      }
    }
    case LeaveCS => {
      // if we have both tokens, for sure the last one was a pong
      // and it is assumed the pingNo and pongNo have already been updated
      sendPing(pingNo)
      sendPong(pongNo)
      context.become(noToken)
    }
  }
}
