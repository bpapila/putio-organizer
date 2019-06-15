package org.papila.organizer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.FileName
import org.papila.organizer.service.Organizer.{Folder, LibraryFolderId}
import org.papila.organizer.service.{Organizer, PutIoService, PutioOrganizer, PutioScanner}

import scala.concurrent.Await
import scala.concurrent.duration._

object GraphPutio extends App with PutioOrganizer {

  implicit val system = ActorSystem("TestSystem")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val putIoClient = new PutioClient(token = "VTQWG4M3LK5I5LD7IL25")
  val scanner: PutioScanner = new PutioScanner(putIoClient)
  val putIoService = new PutIoService(putIoClient)

  // folder structure
  var dict: Map[FileName, Organizer.Folder] = scanner.scan(LibraryFolderId)

  Await.result(
    organize(Folder("Tv Series", LibraryFolderId, dict), putIoClient, putIoService),
    100 seconds
  )
}
