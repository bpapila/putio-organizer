package org.papila.organizer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient._

import scala.concurrent.ExecutionContext

object OrganizerApp extends App {

  implicit val system = ActorSystem("putio")
  implicit val ec = ExecutionContext.global
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  case class Series(name: String, folderId: FolderId, seasons: Map[String, String] = Map.empty)

  val downloadsFolderId = "619201714"
  val tvShowFolderId = "619877202"
  var seriesDict: Map[String, Series] = Map.empty

  val seriesPerPage = "2"
  val seasonPerPage = "2"

  val putioClient = new PutioClient {
    override lazy val token: AccessToken = "VTQWG4M3LK5I5LD7IL25"
  }

  def run(): Unit = {
    scanFolders(tvShowFolderId)

    putioClient.listFiles(downloadsFolderId, FileType.Folder, seriesPerPage).files.foreach { downloadedFolder =>

      putioClient.listFiles(downloadedFolder.id.toString, FileType.Video, seasonPerPage).files.foreach { downloadedVideo =>
        val pattern = "(.+)S(\\d{2})E\\d{2}".r



      }

//
//      var series = Series(seriesFolder.name, seriesFolder.id.toString)
//
//      // scan season folders
//      putioClient.listFiles(seriesFolder.id.toString, FileType.Folder, "5").files.foreach { seasonFolder =>
//        series = series.copy(seasons = series.seasons + (seasonFolder.id.toString -> seasonFolder.name))
//      }
//
//      seriesDict = seriesDict + (series.name -> series)
    }

  }

  def extractName(fileName: FileName): (String, String, String) = {
    val pattern = """(?i)(.+)S(\d{2})E(\d{2}).*""".r

    val pattern(series, season, episode) = fileName

    return (
      series.replaceAll("[\\._-]", " ").split(' ').map(_.capitalize).mkString(" "),
      season,
      episode
    )
  }


  private def scanFolders(folderId: FolderId) = {
    // scan folders
    putioClient.listFiles(folderId, FileType.Folder, "1").files.foreach { seriesFolder =>
      var series = Series(seriesFolder.name, seriesFolder.id.toString)

      // scan season folders
      putioClient.listFiles(seriesFolder.id.toString, FileType.Folder, "5").files.foreach { seasonFolder =>
        series = series.copy(seasons = series.seasons + (seasonFolder.id.toString -> seasonFolder.name))
      }

      seriesDict = seriesDict + (series.name -> series)
    }
  }
}

