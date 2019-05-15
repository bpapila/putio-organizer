package org.papila.organizer.service

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import org.papila.organizer.GraphPutio.putioClient
import org.papila.organizer.client.PutioClient.File
import org.papila.organizer.service.Organizer.Episode
import org.papila.organizer.service.StringUtils.extractSeriesName

import scala.concurrent.ExecutionContext

trait PutioOrganizer {

  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val ec: ExecutionContext

  def filesSource(bufferSize: Int = 100): Source[File, SourceQueueWithComplete[File]] =
    Source.queue[File](100, OverflowStrategy.dropBuffer)

  def fetchFolderFilesFlow(queue: SourceQueueWithComplete[File]) = Flow[File].collect {
    case folder @ File(folderId, _, _, "FOLDER") =>
      listFiles(folderId).foreach(queue.offer)
      folder
    case f @ File(_, _, _, "VIDEO") =>
      f
  }

  def videoFileFilterFlow = Flow[File].collect {
    case f @ File(_, _, _, "VIDEO") => f
  }

  def fileNameExtractorFlow(): Flow[File, (Episode, File), NotUsed] =
    Flow[File].map { f =>
      (extractSeriesName(f.name), f)
    }

  def listFiles(folder: Int): List[File] =
    putioClient.listFiles(folder, None, "10").files
}
