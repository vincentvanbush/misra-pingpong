import akka.actor.{ActorSystem, Props}

/**
  * Created by mike on 05.01.17.
  */
object Main extends App {
  val system = ActorSystem("misra")
  val misraNodes = (0 to 3).map(_ => system.actorOf(Props[Node]))

  (misraNodes, (0 to 3), misraNodes.tail ++ List(misraNodes.head)).zipped.toList.map {
    case (node, index, next) =>
      node ! Initialize(index, next, misraNodes.length)
  }
}
