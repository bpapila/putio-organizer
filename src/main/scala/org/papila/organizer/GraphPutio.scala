package org.papila.organizer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{AccessToken, FileType}
import org.papila.organizer.service.{Organizer, PutioOrganizer}

object GraphPutio extends App with PutioOrganizer {

  override implicit val system = ActorSystem("TestSystem")
  override implicit val materializer = ActorMaterializer()
  override implicit val ec = system.dispatcher

  val putioClient = new PutioClient {
    override lazy val token: AccessToken = "VTQWG4M3LK5I5LD7IL25"
  }

  val (queue, source) = filesSource(10)
    .preMaterialize()

  val sinkPrint = Sink.foreach(println)

  source
    .take(10)
    .via(fetchFolderFilesFlow(queue))
    .via(videoFileFilterFlow)
    .via(fileNameExtractorFlow())
    .log("EXTRACTOR", x => println(s"EXTRACTOR:     $x"))
    .to(Sink.ignore)
    .run()

  putioClient
    .listFiles(Organizer.TvShowFolderId, Some(FileType.Folder), "2")
    .files
    .foreach(queue.offer)
}
