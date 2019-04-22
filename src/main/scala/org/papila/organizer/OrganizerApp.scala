package org.papila.organizer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient._

import scala.concurrent.ExecutionContext

object OrganizerApp extends App {

  import org.papila.organizer.service.StringUtils._

  implicit val system = ActorSystem("putio")
  implicit val ec = ExecutionContext.global
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  case class Series(name: String, folderId: FolderId, seasons: Map[String, String] = Map.empty)

  val downloadsFolderId = 619201714
  val tvShowFolderId = 619877202
  var seriesDict: Map[String, Series] = Map.empty

  val downloadsPerPage = "100"
  val videoPerPage = "100"

  val putioClient = new PutioClient {
    override lazy val token: AccessToken = "VTQWG4M3LK5I5LD7IL25"
  }


  run()
  println(seriesDict)

  def run(): Unit = {
    scanFolders(tvShowFolderId)

    putioClient.listFiles(downloadsFolderId, FileType.Folder, downloadsPerPage).files.foreach { downloadedFolder =>

      putioClient.listFiles(downloadedFolder.id, FileType.Video, videoPerPage).files.foreach { downloadedVideo =>

        val (seriesName, season, episode) = extractName(downloadedVideo.name)
        println(s"Found series = $seriesName season = $season episode $episode")

        var series: Series = null

        // check series root folder there
        seriesDict get seriesName match {
          case None =>
            val folderId = putioClient.createFolder(seriesName, tvShowFolderId).file.id
            series = Series(seriesName, folderId)
            seriesDict = seriesDict + (seriesName -> series)
          case Some(s)  => series = s
        }

        println(s"*** $series")

        val seasonFolderName = "Season " + season
        seriesDict(seriesName).seasons get seasonFolderName match {
          case None =>
            val folderId = putioClient.createFolder(s"Season $season", series.folderId).file.id
            series = series.copy(seasons = series.seasons + (seasonFolderName -> folderId.toString))
            seriesDict = seriesDict + (seriesName -> series)
          case Some(s) =>
            series = series.copy(seasons = series.seasons + (season -> s))
        }

        putioClient.moveFile(downloadedVideo.id, Integer.parseInt(series.seasons(seasonFolderName)))
      }
    }

  }

  private def scanFolders(folderId: FolderId) = {
    // scan folders
    putioClient.listFiles(folderId, FileType.Folder, "999").files.foreach { seriesFolder =>
      var series = Series(seriesFolder.name, seriesFolder.id)

      // scan season folders
      putioClient.listFiles(seriesFolder.id, FileType.Folder, "5").files.foreach { seasonFolder =>
        series = series.copy(seasons = series.seasons + (seasonFolder.name -> seasonFolder.id.toString))
      }

      seriesDict = seriesDict + (series.name -> series)
    }
  }
}
