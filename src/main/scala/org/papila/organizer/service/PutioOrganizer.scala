package org.papila.organizer.service

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{FolderId, PutIoFile}
import org.papila.organizer.service.Organizer._
import org.papila.organizer.service.FileNameParser.{fileToEpisode, nameParsable}

import scala.concurrent.ExecutionContext

trait PutioOrganizer {

  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val ec: ExecutionContext

  def source(bufferSize: Int = 100): Source[PutIoFile, SourceQueueWithComplete[PutIoFile]] =
    Source.queue[PutIoFile](100, OverflowStrategy.dropBuffer)

  /*
   * Input: PutIoFiles
   * Output: PutIoFiles - only of type VIDEO
   * Process: It opens up FOLDERs and queues everything under the FOLDER
   */
  def queueFilesUnderFolderFlow(
                                 queue: SourceQueueWithComplete[PutIoFile],
                                 putIoService: PutIoService
                               ): Flow[PutIoFile, PutIoFile, NotUsed] = Flow[PutIoFile].collect {
    case folder@PutIoFile(folderId, _, _, "FOLDER") =>
      putIoService.offerFilesUnderDir(folderId, queue)
      folder
    case f@PutIoFile(_, _, _, "VIDEO") => f
  }
    .filter((f: PutIoFile) => f.file_type == "VIDEO")


  /*
  * Input: PutIoFiles
  * Output: Episode
  * Process: It tries to extract an Episode from name of the given file
  */
  def fileNameExtractorFlow: Flow[PutIoFile, Episode, NotUsed] =
    Flow[PutIoFile].collect {
      case f@PutIoFile(id, name, parent_id, file_type) if nameParsable(f) => fileToEpisode(f)
    }

  def videoFinderRecursive(
                            putioService: PutIoService,
                            bufferSize: Int = 100
                          ): (SourceQueueWithComplete[PutIoFile], Source[Episode, NotUsed]) = {

    source(bufferSize).preMaterialize() match {
      case (queue, source) =>
        (
          queue,
          source
            .via(queueFilesUnderFolderFlow(queue, putioService))
            .log("VIDEO FILE:", x => println(s"VIDEO FILE:     $x"))
            .via(fileNameExtractorFlow)
            .log("EXTRACTOR", x => println(s"EXTRACTOR:     $x"))
        )
    }
  }

  def organize(root: Folder, client: PutioClient, service: PutIoService) = {
    val (queue, src) = videoFinderRecursive(service)
    val f = src via folderCreatorFlow(root, client) runWith(Sink.ignore)
    service.offerFilesUnderDir(606680222, queue)
    f
  }

  def organizeFoldersFlow(): Flow[Episode, FilesMap, NotUsed] =
    Flow[Episode].fold(Map.empty[String, File])(organizeSeriesIntoFolder)

  def organizeSeriesIntoFolder(fs: FilesMap, e: Episode): Map[String, File] = {

    val fsWithSeries = createFileEntry(fs, e.series)

    val seriesFolder = fsWithSeries(e.series)
    val seasons = seriesFolder.items ++ createFileEntry(seriesFolder.items, e.seasonNo)

    fsWithSeries + (
      e.series -> fsWithSeries(e.series).copy(items = seasons)
      )
  }

  def folderCreatorFlow(root: Folder, putioClient: PutioClient): Flow[Episode, Episode, NotUsed] =
    Flow[Episode].statefulMapConcat { () =>
      var folder = root
      episode => {
        if (!folder.hasSubFolder(episode.series)) {
          val seriesPutioFolder = putioClient.createFolder(episode.series, folder.folderId).file
          folder = folder.addSubFolder(Folder(seriesPutioFolder.name, seriesPutioFolder.id))
        }

        var seriesFolder = folder.items(episode.series)
        val seasonFolder = if (!seriesFolder.hasSubFolder(s"Season ${episode.seasonNo}")) {
          val seasonPutioFolder = putioClient.createFolder(s"Season ${episode.seasonNo}", seriesFolder.folderId).file
          val seasonFolder = Folder(seasonPutioFolder.name, seasonPutioFolder.id)
          seriesFolder = seriesFolder.addSubFolder(seasonFolder)
          seasonFolder
        } else {
          seriesFolder.items(s"Season ${episode.seasonNo}")
        }

        folder = folder.addSubFolder(seriesFolder)

        putioClient.moveFile(episode.file.id, seasonFolder.folderId)

        List(episode)
      }
    }

  def createFileEntry(fs: FilesMap, key: String): FilesMap = {
    fs get key match {
      case None => fs + (key -> File(key))
      case Some(_) => fs
    }
  }
}
