package org.papila.organizer.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.FolderId
import org.papila.organizer.service.StringUtils.extractSeriesName

import scala.concurrent.ExecutionContext

class Organizer(scanner: PutioScanner, putioClient: PutioClient) {

  import Organizer._

  def organize()(implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer) = {

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

object Organizer {

  val DownloadsFolderId = 619201714
  val TvShowFolderId = 619877202

  val downloadsPerPage = "100"
  val videoPerPage = "100"

  case class Series(name: String, folderId: FolderId, seasons: Map[String, String] = Map.empty)

}
