package org.papila.organizer.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{FileListResponse, FileType, PutIoFile}
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike}
import org.scalatest.Matchers._
import org.mockito.Mockito._
import org.papila.organizer.service.Organizer.Folder

import scala.concurrent.ExecutionContext
import org.scalatest.mockito.MockitoSugar.mock

class PutIoSeriesScannerTest extends TestKit(ActorSystem("PutioScannerTest")) with FlatSpecLike with BeforeAndAfterEach {

  implicit val ec = ExecutionContext.global

  val clientImpl = mock[PutioClient]

  val scanner = new PutIoSeriesScanner(clientImpl)

  override def beforeEach() {
    reset(clientImpl)
  }

  val seriesName = "The Series Name"
  val folderId = 100
  val name = "Season 06"
  val downloadsFolderId = 10
  val seasonFolderFile = PutIoFile(folderId, name, downloadsFolderId)
  val seasonFolder = Folder(seasonFolderFile.name, seasonFolderFile.id)
  val series = Folder(seriesName, folderId)
  val perPage = "9999"
  val seriesFolderFile = PutIoFile(folderId, seriesName, 0)

  "addSeason" should "add season from folder to series" in {
    scanner.addSeason(seasonFolderFile, series) shouldBe series.copy(items = Map("Season 06" -> seasonFolder))
  }

  "addSeries" should "return Series with all seasons scanned" in {
    when(clientImpl.listFolders(seriesFolderFile.id))
      .thenReturn(
        FileListResponse(List(PutIoFile(25, "Season 05", downloadsFolderId), PutIoFile(26, "Season 06", downloadsFolderId)), PutIoFile(downloadsFolderId, seriesName, 0), None)
      )

    scanner.addSeries(seriesFolderFile) shouldBe
      Folder(seriesName, folderId, Map("Season 05" -> Folder("Season 05", 25), "Season 06" -> Folder("Season 06", 26)))
  }

  "getDownloadedVideos" should "return downloaded videos" in {
    val videoFileId = 123
    val videoFileName = "video.mkv"
    val videoFile = PutIoFile(videoFileId, videoFileName, folderId)
    val folderName = "folder"

    // mock list download folder
    when(clientImpl.listFolders(downloadsFolderId))
      .thenReturn(FileListResponse(List(PutIoFile(folderId, folderName, downloadsFolderId)), PutIoFile(downloadsFolderId, "Downloads", 0), None))

    // mock list folder
    when(clientImpl.listFiles(folderId, Some(FileType.Video), "999"))
      .thenReturn(FileListResponse(List(videoFile), PutIoFile(folderId, folderName, 0), None))

    scanner.getDownloadedVideos(downloadsFolderId) shouldBe List(videoFile)
  }
}
