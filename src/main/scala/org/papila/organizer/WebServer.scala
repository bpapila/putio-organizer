package org.papila.organizer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.AccessToken
import org.papila.organizer.service.{Organizer, PutioScanner}

object WebServer extends App {

  implicit val system = ActorSystem("PutioOrganizer")
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher

  val putioClient = new PutioClient {
    override lazy val token: AccessToken = "VTQWG4M3LK5I5LD7IL25"
  }

  val scanner = new PutioScanner {
    override val client: PutioClient = putioClient
  }

  val organizer = new Organizer(scanner, putioClient)

  val callbackRoute = path("callback") {
    get {
      organizer.organize()
      complete(StatusCodes.Accepted)
    }
  }

  val routes = callbackRoute

  val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
//
//  bindingFuture
//    .flatMap(_.unbind()) // trigger unbinding from the port
//    .onComplete(_ => system.terminate()) // and shutdown when done


}
