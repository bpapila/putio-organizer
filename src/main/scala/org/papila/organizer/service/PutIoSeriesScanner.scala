package org.papila.organizer.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{FileName, FileType, FolderId, PutIoFile}
import org.papila.organizer.service.Organizer.Folder

import scala.concurrent.ExecutionContext

class PutIoSeriesScanner(val client: PutioClient) {

  def scanSeries(folderId: FolderId)
                (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): Map[FileName, Folder] = {

    client.listFolders(folderId).files.map {
      seriesFolder: PutIoFile => seriesFolder.name -> addSeries(seriesFolder)
    }.toMap
  }

  def addSeries(seriesFolder: PutIoFile)
               (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): Folder = {
    client.listFolders(seriesFolder.id).files
      .foldRight(Folder(seriesFolder.name, seriesFolder.id)){
        case (f: PutIoFile, seasonFolder: Folder) => addSeason(f, seasonFolder)
      }
  }

  def addSeason(seasonFolder: PutIoFile, series: Folder)
               (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): Folder = {
    series.copy(
      items = series.items + (seasonFolder.name -> Folder(seasonFolder.name, seasonFolder.id))
    )
  }

  def getDownloadedVideos(downloadsFolderId: FolderId)
                  (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): List[PutIoFile] = {
    client.listFolders(downloadsFolderId).files.flatMap {
      downloadedFolder: PutIoFile =>
        client.listFiles(downloadedFolder.id, Some(FileType.Video), "999").files
    }
  }
}
