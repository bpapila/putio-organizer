package org.papila.organizer.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{File, FileId, FolderId}
import org.papila.organizer.service.StringUtils.extractSeriesName

import scala.concurrent.ExecutionContext

class Organizer(scanner: PutioScanner, putioClient: PutioClient) {

  import Organizer._

  def organize()(implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer) = {

    var dict = scanner.scan(LibraryFolder)

    scanner.getDownloadedVideos(DownloadsFolderId)
      .foreach { file =>

        val episode = extractSeriesName(file.name)
        println(episode.series, episode.season, episode)

        var series: Series = null

        // check series root folder there
        dict get episode.series match {
          case None =>
            val folderId = putioClient.createFolder(episode.series, LibraryFolder).file.id
            series = Series(episode.series, folderId)
            dict = dict + (episode.series -> series)
          case Some(s) => series = s
        }

        val seasonFolderName = "Season " + episode.season
        dict(episode.series).seasons get seasonFolderName match {
          case None =>
            val folderId = putioClient.createFolder(s"Season ${episode.season}", series.folderId).file.id
            series = series.copy(seasons = series.seasons + (seasonFolderName -> folderId.toString))
            dict = dict + (episode.series -> series)
          case Some(s) =>
            series = series.copy(seasons = series.seasons + (episode.season -> s))
        }

        putioClient.moveFile(file.id, Integer.parseInt(series.seasons(seasonFolderName)))

      }
  }
}

object Organizer {

  val DownloadsFolderId = 619201714
  val LibraryFolder = 619877202

  val downloadsPerPage = "100"
  val videoPerPage = "100"

  type FolderContents = Map[String, Folder]
  type EpisodeWithFile = (Episode, File)

  case class Series(name: String, folderId: FolderId, seasons: Map[String, String] = Map.empty, localIdentifier: String = "")
  case class Episode(series: String, season: String, episode: String)
  case class Folder(name: String, items: FolderContents = Map.empty, parentId: Option[Int] = None, id: Option[Int] = None)

  sealed trait PutIoTask {
    def run(client: PutioClient)(implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): File
  }

  case class CreateFolderTask(name: String, parentId: FolderId) extends PutIoTask {
    override def run(client: PutioClient)
                    (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): File =
      client.createFolder(name, parentId).file
  }

  case class MoveTask(fileId: FileId, toFolderId: FileId) extends PutIoTask {
    override def run(client: PutioClient)(implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): File =
      client.moveFile(fileId, toFolderId)
  }

}
