import akka.actor.ActorSystem
import akka.event.Logging.Debug
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.papila.organizer.WebServer.system

import scala.concurrent.ExecutionContextExecutor

implicit val system: ActorSystem = ActorSystem("asd")
implicit val ec: ExecutionContextExecutor = system.dispatcher

val s = Source(List(1, 2))
val f = Flow[Int].fold(0) { (sum, i) => sum + i}
val sink = Sink.ignore

val res = s via f runWith sink
res.map (_ => Console.println("Finished"))


