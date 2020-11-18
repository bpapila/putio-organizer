package org.papila.organizer.service

import akka.actor.ActorSystem
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{FileId, FolderId, PutIoFile}
import org.papila.organizer.service.FileNameParser.fileToEpisode

import scala.concurrent.ExecutionContext

class Organizer(scanner: PutIoSeriesScanner, putioClient: PutioClient) {

  import Organizer._

  def organize()(implicit ec: ExecutionContext, system: ActorSystem) = {

    var dict = scanner.scanSeries(LibraryFolderId)

    scanner.getDownloadedVideos(DownloadsFolderId)
      .foreach { file =>

        val episode = fileToEpisode(file)
        println(episode.showName, episode.seasonNo, episode)

        var series: Folder = null

        // check series root folder there
        dict get episode.showName match {
          case None =>
            val folderId = putioClient.createFolder(episode.showName, LibraryFolderId).file.id
            series = Folder(episode.showName, folderId)
            dict = dict + (episode.showName -> series)
          case Some(s) => series = s
        }

        val seasonFolderName = "Season " + episode.seasonNo
        dict(episode.showName).items get seasonFolderName match {
          case None =>
            val createdFolder = putioClient.createFolder(s"Season ${episode.seasonNo}", series.folderId).file
            series = series.copy(items = series.items + (seasonFolderName -> Folder(createdFolder.name, createdFolder.id)))
            dict = dict + (episode.showName -> series)
          case Some(s) =>
            series = series.copy(items = series.items + (episode.seasonNo -> s))
        }

        putioClient.moveFile(file.id, series.items(seasonFolderName).folderId)

      }
  }
}

object Organizer {

  val DownloadsFolderId = 619201714
  val LibraryFolderId = 619877202

  val downloadsPerPage = "100"
  val videoPerPage = "100"

  type FilesMap = Map[String, File]

  case class ShowsLibrary(showsFolder: Folder) {
    val libraryFolderId = showsFolder.folderId
    def getShow(showName: String): Option[Folder] = showsFolder.items.get(showName)
    def getSeason(showName: String, seasonFolderName: String): Option[Folder] = {
      println("###", "Getting season", showName, seasonFolderName)
      println("###", showsFolder)
      showsFolder.items(showName).items.get(seasonFolderName)
    }
    def addShow(f: Folder): ShowsLibrary = ShowsLibrary(showsFolder.addSubFolder(f))
    def addSeason(showName: String, seasonFolder: Folder): ShowsLibrary = ShowsLibrary(showsFolder.items(showName).addSubFolder(seasonFolder))
  }
  object ShowsLibrary {
    def apply(f: Folder): ShowsLibrary = new ShowsLibrary(f)
  }

  case class Folder(name: String, folderId: FolderId, items: Map[String, Folder] = Map.empty) {
    def addSubFolder(folder: Folder): Folder = items.get(name) match {
      case None => this.copy(items = folder.items + (folder.name -> folder))
      case Some(_) => this.copy()
    }

    def hasSubFolder(folderName: String): Boolean = items.get(folderName) match {
      case None => false
      case Some(_) => true
    }

    def getShow(showName: String): Folder = {
      items(showName)
    }
  }

  case class Episode(showName: String, seasonNo: String, episode: String, file: PutIoFile) {
    val seasonFolderName = s"Season ${seasonNo}"
  }

  case class File(
                   name: String,
                   items: FilesMap = Map.empty,
                   parentId: Option[Int] = None,
                   id: Option[Int] = None
                 )

  sealed trait PutIoTask {
    def run(client: PutioClient)(implicit ec: ExecutionContext, system: ActorSystem): PutIoFile
  }

  case class CreateFolderTask(name: String, parentId: FolderId) extends PutIoTask {
    override def run(client: PutioClient)
                    (implicit ec: ExecutionContext, system: ActorSystem): PutIoFile =
      client.createFolder(name, parentId).file
  }

  case class MoveTask(fileId: FileId, toFolderId: FileId) extends PutIoTask {
    override def run(client: PutioClient)(implicit ec: ExecutionContext, system: ActorSystem): PutIoFile =
      client.moveFile(fileId, toFolderId)
  }

}
