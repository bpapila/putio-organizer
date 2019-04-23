package org.papila.organizer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient._
import org.papila.organizer.service.PutioScanner

import scala.concurrent.ExecutionContext

object OrganizerApp extends App {

  import org.papila.organizer.service.StringUtils._

  implicit val system = ActorSystem("putio")
  implicit val ec = ExecutionContext.global
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  case class Series(name: String, folderId: FolderId, seasons: Map[String, String] = Map.empty)

  val DownloadsFolderId = 619201714
  val TvShowFolderId = 619877202

  val downloadsPerPage = "100"
  val videoPerPage = "100"

  val putioClient = new PutioClient {
    override lazy val token: AccessToken = "VTQWG4M3LK5I5LD7IL25"
  }

  val scanner = new PutioScanner {
    override val client: PutioClient = putioClient
  }

  run()

  def run() = {

    var dict = scanner.scan(TvShowFolderId)

    scanner.getDownloadedVideos(DownloadsFolderId)
      .foreach{ file =>

        val (seriesName, season, episode) = extractSeriesName(file.name)
        println(seriesName, season, episode)

        var series: Series = null

        // check series root folder there
        dict get seriesName match {
          case None =>
            val folderId = putioClient.createFolder(seriesName, TvShowFolderId).file.id
            series = Series(seriesName, folderId)
            dict = dict + (seriesName -> series)
          case Some(s)  => series = s
        }

        val seasonFolderName = "Season " + season
        dict(seriesName).seasons get seasonFolderName match {
          case None =>
            val folderId = putioClient.createFolder(s"Season $season", series.folderId).file.id
            series = series.copy(seasons = series.seasons + (seasonFolderName -> folderId.toString))
            dict = dict + (seriesName -> series)
          case Some(s) =>
            series = series.copy(seasons = series.seasons + (season -> s))
        }

        putioClient.moveFile(file.id, Integer.parseInt(series.seasons(seasonFolderName)))

      }
  }
}
