package org.papila.organizer.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.papila.organizer.OrganizerApp.Series
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{File, FileListResponse, FileType}
import org.scalatest.{BeforeAndAfterEach, FlatSpec, FlatSpecLike}
import org.scalatest.Matchers._
import org.mockito.Mockito._

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
  val parentId = 10
  val seasonFolder = File(folderId, name, parentId)
  val series = Series(seriesName, folderId)

  val seriesFolder = File(folderId, seriesName, 0)

  "addSeason" should "add season from folder to series" in {
    scanner.addSeason(seasonFolder, series) shouldBe series.copy(seasons = Map("Season 06" -> folderId.toString))
  }

  "addSeries" should "return Series with all seasons scanned" in {
    when(clientImpl.listFiles(seriesFolder.id, FileType.Folder, "5"))
      .thenReturn(
        FileListResponse(List(File(25, "Season 05", parentId), File(26, "Season 06", parentId)), File(parentId, seriesName, 0), None)
      )

    scanner.addSeries(seriesFolder) shouldBe Series(seriesName, folderId, Map(("Season 05" -> "25"), ("Season 06" -> "26")))
  }

}
