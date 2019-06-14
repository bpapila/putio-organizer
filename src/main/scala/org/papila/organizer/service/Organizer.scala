package org.papila.organizer.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{PutIoFile, FileId, FolderId}
import org.papila.organizer.service.StringUtils.fileToEpisode

import scala.concurrent.ExecutionContext

class Organizer(scanner: PutioScanner, putioClient: PutioClient) {

  import Organizer._

  def organize()(implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer) = {

    var dict = scanner.scan(LibraryFolder)

    scanner.getDownloadedVideos(DownloadsFolderId)
      .foreach { file =>

        val episode = fileToEpisode(file)
        println(episode.series, episode.seasonNo, episode)

        var series: Folder = null

        // check series root folder there
        dict get episode.series match {
          case None =>
            val folderId = putioClient.createFolder(episode.series, LibraryFolder).file.id
            series = Folder(episode.series, folderId)
            dict = dict + (episode.series -> series)
          case Some(s) => series = s
        }

        val seasonFolderName = "Season " + episode.seasonNo
        dict(episode.series).items get seasonFolderName match {
          case None =>
            val createdFolder = putioClient.createFolder(s"Season ${episode.seasonNo}", series.folderId).file
            series = series.copy(items = series.items + (seasonFolderName -> Folder(createdFolder.name, createdFolder.id)))
            dict = dict + (episode.series -> series)
          case Some(s) =>
            series = series.copy(items = series.items + (episode.seasonNo -> s))
        }

        putioClient.moveFile(file.id, series.items(seasonFolderName).folderId)

      }
  }
}

object Organizer {

  val DownloadsFolderId = 619201714
  val LibraryFolder = 619877202

  val downloadsPerPage = "100"
  val videoPerPage = "100"

  type FilesMap = Map[String, File]

  case class Folder(name: String, folderId: FolderId, items: Map[String, Folder] = Map.empty) {
    def addSubFolder(folder: Folder): Folder = items.get(name) match {
      case None => this.copy(items = folder.items + (folder.name -> folder))
      case Some(_) => this.copy()
    }

    def hasSubFolder(folderName: String): Boolean = items.get(folderName) match {
      case None => false
      case Some(_) => true
    }
  }
  case class Episode(series: String, seasonNo: String, episode: String, file: PutIoFile)

  case class File(
                   name: String,
                   items: FilesMap = Map.empty,
                   parentId: Option[Int] = None,
                   id: Option[Int] = None
                 )

  sealed trait PutIoTask {
    def run(client: PutioClient)(implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): PutIoFile
  }

  case class CreateFolderTask(name: String, parentId: FolderId) extends PutIoTask {
    override def run(client: PutioClient)
                    (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): PutIoFile =
      client.createFolder(name, parentId).file
  }

  case class MoveTask(fileId: FileId, toFolderId: FileId) extends PutIoTask {
    override def run(client: PutioClient)(implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): PutIoFile =
      client.moveFile(fileId, toFolderId)
  }

}
