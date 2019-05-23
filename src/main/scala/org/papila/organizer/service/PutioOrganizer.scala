package org.papila.organizer.service

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, OverflowStrategy, QueueOfferResult}
import org.papila.organizer.GraphPutio.putioClient
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{File, FileId, FileType}
import org.papila.organizer.service.Organizer.Episode
import org.papila.organizer.service.StringUtils.extractSeriesName

import scala.concurrent.{ExecutionContext, Future}

trait PutioOrganizer {

  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val ec: ExecutionContext

  def filesSource(bufferSize: Int = 100): Source[File, SourceQueueWithComplete[File]] =
    Source.queue[File](100, OverflowStrategy.dropBuffer)

  def fetchFolderFilesRecFlow(queue: SourceQueueWithComplete[File], putIoService: PutioService) = Flow[File].collect {
    case folder@File(folderId, _, _, "FOLDER") =>
      putIoService.offerFilesUnderDir(folderId, queue)
      folder
    case f@File(_, _, _, "VIDEO") =>
      f
  }

  def videoFileFilter = Flow[File].collect {
    case f@File(_, _, _, "VIDEO") => f
  }

  def fileNameExtractor: Flow[File, (Episode, File), NotUsed] =
    Flow[File].map { f =>
      (extractSeriesName(f.name), f)
    }

  def videoFinderRecursive(
                          putioService: PutioService,
                            bufferSize: Int = 100
                          ): (SourceQueueWithComplete[File], Source[(Episode, File), NotUsed]) = {

    filesSource(bufferSize).preMaterialize() match {
      case (queue, source) =>
        (
          queue,
          source
            .log("SOURCE", x => println(s"SOURCE:     $x"))
            .via(fetchFolderFilesRecFlow(queue, putioService))
            .via(videoFileFilter)
            .via(fileNameExtractor)
            .log("EXTRACTOR", x => println(s"EXTRACTOR:     $x"))
        )
    }
  }
}

class PutioService(client: PutioClient)
                  (implicit val system: ActorSystem,
                   materializer: ActorMaterializer,
                   ec: ExecutionContext) {
  def offerFilesUnderDir(id: FileId, srcQueue: SourceQueueWithComplete[File]): Unit =
    client.listFiles(id, None, "10").files
      .foreach(srcQueue.offer)
}
