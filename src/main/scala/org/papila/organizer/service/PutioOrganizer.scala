package org.papila.organizer.service

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.PutIoFile
import org.papila.organizer.service.FileNameParser.{fileToEpisode, nameParsable}
import org.papila.organizer.service.Organizer._

import scala.concurrent.ExecutionContext

trait PutioOrganizer {

  implicit val system: ActorSystem
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

  def allVideosSource(
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
    val (queue, src) = allVideosSource(service)
    val f = src via folderCreatorFlow(root, client) runWith (Sink.ignore)
    service.offerFilesUnderDir(606680222, queue)
    f
  }

  /*
  * Input: Episode
  * Output: Episode
  * Process: Checks if given episode has a folder. If not creates and updates the Folder. Folder is mutated.
  */
  def folderCreatorFlow(root: Folder, putioClient: PutioClient): Flow[Episode, ShowsLibrary, NotUsed] = {
    def createAddSeries(library: ShowsLibrary, episode: Episode) = {
      val seriesPutioFolder = putioClient.createFolder(episode.showName, parentId = library.libraryFolderId).file
      val temp = library.addShow(Folder(seriesPutioFolder.name, seriesPutioFolder.id))
      temp
    }

    def createAddSeason(library: ShowsLibrary, episode: Episode) = {
      val seasonPutioFolder = putioClient.createFolder(episode.seasonFolderName, library.getShow(episode.showName).get.folderId).file
      Console.println("*******", seasonPutioFolder)
      val temp = library.addSeason(episode.showName, Folder(seasonPutioFolder.name, seasonPutioFolder.id))
      temp
    }

    Flow[Episode].fold(ShowsLibrary(root)) { (library, episode) =>
      Console.println("STARTING", "library", library)
      var updatedLibrary = library.getShow(episode.showName) match {
        case None => createAddSeries(library, episode)
        case Some(f) => library
      }

      val bla = updatedLibrary.getSeason(episode.showName, episode.seasonFolderName) match {
        case None => createAddSeason(library, episode)
        case Some(f) => library
      }

      println("FINAL:" , updatedLibrary)

      updatedLibrary.getSeason(episode.showName, episode.seasonFolderName).map { f =>
        Console.println("MOVING FILES", episode)
        putioClient.moveFile(episode.file.id, f.folderId)
        updatedLibrary
      }.get
    }
  }

  def createAndAddToLibrary(library: ShowsLibrary, )

  //    Flow[Episode].statefulMapConcat { () =>
  //      var folder = root
  //      episode => {
  //
  //        if (!folder.hasSubFolder(episode.series)) {
  //          // Create series folder under root
  //          val seriesPutioFolder = putioClient.createFolder(episode.series, folder.folderId).file
  //          folder = folder.addSubFolder(Folder(seriesPutioFolder.name, seriesPutioFolder.id))
  //        }
  //
  //        var seriesFolder = folder.items(episode.series)
  //
  //        if (!seriesFolder.hasSubFolder(episode.seasonFolderName)) {
  //          // Create season folder
  //          val seasonPutioFolder = putioClient.createFolder(episode.seasonFolderName, seriesFolder.folderId).file
  //          seriesFolder = seriesFolder.addSubFolder(Folder(seasonPutioFolder.name, seasonPutioFolder.id))
  //        }
  //
  //        folder = folder.addSubFolder(seriesFolder)
  //
  //        val seasonFolderId = seriesFolder.items(episode.seasonFolderName).folderId
  //        putioClient.moveFile(episode.file.id, seasonFolderId)
  //
  //        List(episode)
  //      }
  //    }

  def createFileEntry(fs: FilesMap, key: String): FilesMap = {
    fs.get(key) match {
      case None => fs + (key -> File(key))
      case Some(_) => fs
    }
  }
}
