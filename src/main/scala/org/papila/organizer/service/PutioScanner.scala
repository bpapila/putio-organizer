package org.papila.organizer.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{PutIoFile, FileType, FolderId}
import org.papila.organizer.service.Organizer.Series

import scala.concurrent.ExecutionContext

class PutioScanner(val client: PutioClient) {

  def scan(folderId: FolderId)
          (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer) = {

    client.listFiles(folderId, Some(FileType.Folder), "999").files.map {
      seriesFolder: PutIoFile => seriesFolder.name -> addSeries(seriesFolder)
    }.toMap
  }

  def addSeries(folder: PutIoFile)
               (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): Series = {
    client.listFiles(folder.id, Some(FileType.Folder), "999").files
      .foldRight(Series(folder.name, folder.id)){
        case (f: PutIoFile, s: Series) => addSeason(f, s)
      }
  }

  def addSeason(folder: PutIoFile, series: Series)
               (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): Series = {
    series.copy(seasons = series.seasons + (folder.name -> folder.id.toString))
  }

  def getShows(folderId: FolderId)
              (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): Map[String, Series] =
    client.listFiles(folderId, Some(FileType.Folder), "999").files.map {seriesFolder =>
      seriesFolder.name -> Series(seriesFolder.name, seriesFolder.id)
    }.toMap

  def getDownloadedVideos(folderId: FolderId)
                  (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): List[PutIoFile] = {
    client.listFiles(folderId, Some(FileType.Folder), "999").files.flatMap {
      downloadedFolder: PutIoFile =>
        client.listFiles(downloadedFolder.id, Some(FileType.Video), "999").files
    }
  }
}
