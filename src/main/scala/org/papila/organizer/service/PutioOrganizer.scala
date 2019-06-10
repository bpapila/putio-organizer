package org.papila.organizer.service

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import org.papila.organizer.client.PutioClient.PutIoFile
import org.papila.organizer.service.Organizer._
import org.papila.organizer.service.StringUtils.fileToEpisode

import scala.concurrent.ExecutionContext

trait PutioOrganizer {

  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val ec: ExecutionContext

  def filesSource(bufferSize: Int = 100): Source[PutIoFile, SourceQueueWithComplete[PutIoFile]] =
    Source.queue[PutIoFile](100, OverflowStrategy.dropBuffer)

  def fetchFolderFilesRecFlow(queue: SourceQueueWithComplete[PutIoFile], putIoService: PutIoService) = Flow[PutIoFile].collect {
    case folder@PutIoFile(folderId, _, _, "FOLDER") =>
      putIoService.offerFilesUnderDir(folderId, queue)
      folder
    case f@PutIoFile(_, _, _, "VIDEO") =>
      f
  }

  def videoFileFilter = Flow[PutIoFile].collect {
    case f@PutIoFile(_, _, _, "VIDEO") => f
  }

  def fileNameExtractor: Flow[PutIoFile, Episode, NotUsed] =
    Flow[PutIoFile].map(f => fileToEpisode(f))

  def videoFinderRecursive(
                            putioService: PutIoService,
                            bufferSize: Int = 100
                          ): (SourceQueueWithComplete[PutIoFile], Source[Episode, NotUsed]) = {

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

  def organizeFoldersFlow(): Flow[Episode, FilesMap, NotUsed] =
    Flow[Episode].fold(Map.empty[String, File])(organizeSeriesIntoFolder)

  def organizeSeriesIntoFolder(fs: FilesMap, e: Episode): Map[String, File] = {

    val fsWithSeries = createFileEntry(fs, e.series)

    val seriesFolder = fsWithSeries(e.series)
    val seasons = seriesFolder.items ++ createFileEntry(seriesFolder.items, e.season)

    fsWithSeries + (
      e.series -> fsWithSeries(e.series).copy(items = seasons)
      )
  }

  def createFileEntry(fs: FilesMap, key: String): FilesMap = {
    fs get key match {
      case None => fs + (key -> File(key))
      case Some(_) => fs
    }
  }

//  def folderCreator()

}


