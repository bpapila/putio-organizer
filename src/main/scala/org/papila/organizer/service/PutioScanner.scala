package org.papila.organizer.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{File, FileType, FolderId}
import org.papila.organizer.service.Organizer.Series

import scala.concurrent.ExecutionContext

trait PutioScanner {

  val client: PutioClient

  def scan(folderId: FolderId)
          (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer) = {

    client.listFiles(folderId, FileType.Folder, "999").files.map {
      seriesFolder: File => seriesFolder.name -> addSeries(seriesFolder)
    }.toMap
  }

  def addSeries(folder: File)
               (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): Series = {
    client.listFiles(folder.id, FileType.Folder, "999").files
      .foldRight(Series(folder.name, folder.id)){
        case (f: File, s: Series) => addSeason(f, s)
      }
  }

  def addSeason(folder: File, series: Series)
               (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): Series = {
    series.copy(seasons = series.seasons + (folder.name -> folder.id.toString))
  }

  def getShows(folderId: FolderId)
              (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): Map[String, Series] =
    client.listFiles(folderId, FileType.Folder, "999").files.map {seriesFolder =>
      seriesFolder.name -> Series(seriesFolder.name, seriesFolder.id)
    }.toMap

  def getDownloadedVideos(folderId: FolderId)
                  (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): List[File] = {
    client.listFiles(folderId, FileType.Folder, "999").files.flatMap {
      downloadedFolder: File =>
        client.listFiles(downloadedFolder.id, FileType.Video, "999").files.map {
          downloadedVideo: File => downloadedVideo
        }
    }
  }
}
