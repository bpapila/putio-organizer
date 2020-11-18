package org.papila.organizer

import akka.actor.ActorSystem
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.FileName
import org.papila.organizer.service.Organizer.{Folder, LibraryFolderId}
import org.papila.organizer.service.{Organizer, PutIoSeriesScanner, PutIoService, PutioOrganizer}

import scala.concurrent.Await
import scala.concurrent.duration._

object GraphPutio extends App with PutioOrganizer {

  implicit val system = ActorSystem("TestSystem")
  implicit val ec = system.dispatcher

  val putIoClient = new PutioClient(token = "VTQWG4M3LK5I5LD7IL25")
  val scanner: PutIoSeriesScanner = new PutIoSeriesScanner(putIoClient)
  val putIoService = new PutIoService(putIoClient)

  // folder structure
  var dict: Map[FileName, Organizer.Folder] = scanner.scanSeries(LibraryFolderId)

  Await.result(
    organize(
      Folder("Tv Series", 606680222, dict), putIoClient, putIoService
    ),
    100 seconds
  )
}
