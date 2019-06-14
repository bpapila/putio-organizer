package org.papila.organizer.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{FileName, FileType, FolderId, PutIoFile}
import org.papila.organizer.service.Organizer.Folder

import scala.concurrent.ExecutionContext

class PutioScanner(val client: PutioClient) {

  def scan(folderId: FolderId)
          (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): Map[FileName, Folder] = {

    client.listFiles(folderId, Some(FileType.Folder), "999").files.map {
      seriesFolder: PutIoFile => seriesFolder.name -> addSeries(seriesFolder)
    }.toMap
  }

  def addSeries(folder: PutIoFile)
               (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): Folder = {
    client.listFiles(folder.id, Some(FileType.Folder), "999").files
      .foldRight(Folder(folder.name, folder.id)){
        case (f: PutIoFile, s: Folder) => addSeason(f, s)
      }
  }

  def addSeason(folder: PutIoFile, series: Folder)
               (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): Folder = {
    series.copy(items = series.items + (folder.name -> Folder(folder.name, folder.id)))
  }

  def getShows(folderId: FolderId)
              (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): Map[String, Folder] =
    client.listFiles(folderId, Some(FileType.Folder), "999").files.map {seriesFolder =>
      seriesFolder.name -> Folder(seriesFolder.name, seriesFolder.id)
    }.toMap

  def getDownloadedVideos(folderId: FolderId)
                  (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): List[PutIoFile] = {
    client.listFiles(folderId, Some(FileType.Folder), "999").files.flatMap {
      downloadedFolder: PutIoFile =>
        client.listFiles(downloadedFolder.id, Some(FileType.Video), "999").files
    }
  }
}
