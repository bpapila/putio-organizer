package org.papila.organizer.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{File, FileListResponse, FileType}
import org.scalatest.{BeforeAndAfterEach, FlatSpec, FlatSpecLike}
import org.scalatest.Matchers._
import org.mockito.Mockito._
import org.papila.organizer.service.Organizer.Series

import scala.concurrent.ExecutionContext
import org.scalatest.mockito.MockitoSugar.mock

class PutioScannerTest extends TestKit(ActorSystem("PutioScannerTest")) with FlatSpecLike with BeforeAndAfterEach {

  implicit val ec = ExecutionContext.global
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val clientImpl = mock[PutioClient]

  val scanner = new PutioScanner {
    override val client: PutioClient = clientImpl
  }

  override def beforeEach() {
    reset(clientImpl)
  }

  val seriesName = "The Series Name"
  val folderId = 100
  val name = "Season 06"
  val downloadsFolderId = 10
  val seasonFolder = File(folderId, name, downloadsFolderId)
  val series = Series(seriesName, folderId)
  val perPage = "9999"
  val seriesFolder = File(folderId, seriesName, 0)

  "addSeason" should "add season from folder to series" in {
    scanner.addSeason(seasonFolder, series) shouldBe series.copy(seasons = Map("Season 06" -> folderId.toString))
  }

  "addSeries" should "return Series with all seasons scanned" in {
    when(clientImpl.listFiles(seriesFolder.id, Some(FileType.Folder), "999"))
      .thenReturn(
        FileListResponse(List(File(25, "Season 05", downloadsFolderId), File(26, "Season 06", downloadsFolderId)), File(downloadsFolderId, seriesName, 0), None)
      )

    scanner.addSeries(seriesFolder) shouldBe Series(seriesName, folderId, Map("Season 05" -> "25", "Season 06" -> "26"))
  }

  "getDownloadedVideos" should "return downloaded videos" in {
    val videoFileId = 123
    val videoFileName = "video.mkv"
    val videoFile = File(videoFileId, videoFileName, folderId)
    val folderName = "folder"

    // mock list download folder
    when(clientImpl.listFiles(downloadsFolderId, Some(FileType.Folder), "999"))
      .thenReturn(FileListResponse(List(File(folderId, folderName, downloadsFolderId)), File(downloadsFolderId, "Downloads", 0), None))

    // mock list folder
    when(clientImpl.listFiles(folderId, Some(FileType.Video), "999"))
      .thenReturn(FileListResponse(List(videoFile), File(folderId, folderName, 0), None))

    scanner.getDownloadedVideos(downloadsFolderId) shouldBe List(videoFile)
  }
}
