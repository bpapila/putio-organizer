package org.papila.organizer.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.papila.organizer.OrganizerApp.{Series, putioClient, seriesDict}
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{File, FileType, FolderId}

import scala.concurrent.ExecutionContext

trait PutioScanner {

  val client: PutioClient

  def scan(folderId: FolderId)
          (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer) = {

    client.listFiles(folderId, FileType.Folder, "999").files.foreach {
      seriesFolder: File => addSeries(seriesFolder)
    }
  }

  def addSeries(folder: File)
               (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): Series = {
    client.listFiles(folder.id, FileType.Folder, "5").files
      .foldRight(Series(folder.name, folder.id)){
        case (f: File, s: Series) => addSeason(f, s)
      }
  }

  def addSeason(folder: File, series: Series)
               (implicit ec: ExecutionContext, system: ActorSystem, mat: ActorMaterializer): Series = {
    series.copy(seasons = series.seasons + (folder.name -> folder.id.toString))
  }
}
