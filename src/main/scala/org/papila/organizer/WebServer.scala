package org.papila.organizer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.FileName
import org.papila.organizer.service.Organizer.{Folder, LibraryFolderId}
import org.papila.organizer.service.{Organizer, PutIoService, PutioOrganizer, PutIoSeriesScanner}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object WebServer extends App with PutioOrganizer {

  implicit val system: ActorSystem = ActorSystem("PutioOrganizer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val putioClient = new PutioClient("VTQWG4M3LK5I5LD7IL25")
  val putIoService = new PutIoService(putioClient)
  val scanner = new PutIoSeriesScanner(putioClient)

  val callbackRoute = path("callback") {
    post {
      Future {
        val dict: Map[FileName, Organizer.Folder] = scanner.scanSeries(LibraryFolderId)
        organize(Folder("Tv Series", LibraryFolderId, dict), putioClient, putIoService)
      } onComplete {
        case Failure(e) => println("Failed to organize: " + e.getMessage)
        case Success(_) => println("Organized")
      }

      complete(StatusCodes.Accepted)
    }
  }

  val routes = callbackRoute

  val port = sys.env("PORT")

  val bindingFuture = Http().bindAndHandle(routes, "0.0.0.0", Integer.parseInt(port))

  println(s"Server online at http://0.0.0.0:$port/" + s"")

}
