package org.papila.organizer.service

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl.{Keep, Sink, Source, SourceQueueWithComplete}
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar.mock
import org.papila.organizer.GraphPutio
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{File, FileType}
import org.scalatest.{BeforeAndAfter, FlatSpec}
import org.mockito.Mockito.{never, reset, times, verify}
import org.papila.organizer.service.Organizer.{Episode, EpisodeWithFile, Folder, FolderContents}

import scala.concurrent.duration._
import org.scalatest.Matchers._

import scala.concurrent.{Await, ExecutionContext, Future}

class PutioOrganizerSpec extends FlatSpec with BeforeAndAfter {

  import Fixtures._

  implicit val systemImpl = ActorSystem("TestSystem")
  implicit val materializerImpl = ActorMaterializer()
  implicit val ecImpl = systemImpl.dispatcher

  val putioClient = mock[PutioClient]
  val queue = mock[SourceQueueWithComplete[File]]
  val putIoService = mock[PutIoService]

  var putioOrganizer = new PutioOrganizer {
    override implicit val system: ActorSystem = systemImpl
    override implicit val materializer: ActorMaterializer = materializerImpl
    override implicit val ec: ExecutionContext = ecImpl
  }

  before {
    reset(putioClient)
    reset(queue)
    reset(putIoService)
  }

  "filesSource" should "return queue source" in {
    val source = putioOrganizer.filesSource(100)

    val (queue, f) = source.take(2).toMat(Sink.seq[File])(Keep.both).run()
    queue.offer(Folder1)
    queue.offer(Folder2)

    Await.result(f, 3 seconds) shouldBe Seq(Folder1, Folder2)
  }

  behavior of "fetchFolderFilesRecFlow"

  it should "pass on "

  it should "call offerFilesUnderDir on folders" in {

    val flowUnderTest = putioOrganizer.fetchFolderFilesRecFlow(queue, putIoService)

    val f = Source[File](List(Folder1, Folder2, File1))
      .via(flowUnderTest)
      .toMat(Sink.seq)(Keep.right)
      .run()

    Await.result(f, 3 seconds)

    verify(putIoService, times(1))
      .offerFilesUnderDir(Folder1.id, queue)
    verify(putIoService, times(1))
      .offerFilesUnderDir(Folder2.id, queue)
    verify(putIoService, never())
      .offerFilesUnderDir(File1.id, queue)
  }

  it should "pass on files and folders" in {

    val flowUnderTest = putioOrganizer.fetchFolderFilesRecFlow(queue, putIoService)

    val f = Source[File](List(Folder1, Folder2, File1))
      .via(flowUnderTest)
      .toMat(Sink.seq)(Keep.right)
      .run()

    val seq = Await.result(f, 3 seconds)
    seq shouldBe Seq(Folder1, Folder2, File1)
  }

  "videoFileFilter" should "filter only video files" in {
    val flowUnderTest = putioOrganizer.videoFileFilter

    val f = Source[File](List(Folder1, Folder2, File1))
      .via(flowUnderTest)
      .toMat(Sink.seq)(Keep.right)
      .run()

    val seq = Await.result(f, 3 seconds)
    seq shouldBe Seq(File1)
  }

  "createInFolder" should "create key under FolderContents" in {
    val fc: FolderContents = Map.empty[String, Folder]
    GraphPutio.createInFolder(fc, "test")("test") shouldBe Folder("test")
  }

  "createInFolder" should "not duplicate item when key already exist" in {
    val fc: FolderContents = Map.empty[String, Folder]
    fc + ("test" -> Folder("test"))

    val fcUpdated = GraphPutio.createInFolder(fc, "test")
    fcUpdated.size shouldBe 1
    fcUpdated("test") shouldBe Folder("test")
  }

  "organizeSeriesIntoFolder" should "create subfolder" in {
    val fc: FolderContents = Map.empty[String, Folder]

    val episode = Episode("Six Feet Under", "2", "1")
    val episodeWithFile: EpisodeWithFile = (episode, mock[File])

    val updatedFc = GraphPutio.organizeSeriesIntoFolder(fc, episodeWithFile)

    updatedFc(episode.series) shouldBe
      Folder(episode.series, Map(episode.season -> Folder(episode.season)))
  }

  "organizeFoldersFlow" should "organize files into folder structure" in {

    val testList = List(
      (Episode1, mock[File]),
      (Episode2, mock[File]),
      (Episode3, mock[File]),
      (Episode4, mock[File])
    )

    val res = Source(testList)
      .via(GraphPutio.organizeFoldersFlow())
      .toMat(Sink.head)(Keep.right)
      .run()

    val result = Await.result(res, 3 seconds)
    result shouldBe FolderContents

  }
}

object Fixtures {

  val Folder1 = File(123123, "Folder1", 123, FileType.Folder.toString)
  val Folder2 = File(123456, "Folder2", 123, FileType.Folder.toString)
  val File1 = File(123789, "File1", 123456, FileType.Video.toString)

  val Episode1 =Episode("Six Feet Under", "02", "01")
  val Episode2 =Episode("Six Feet Under", "02", "02")
  val Episode3 =Episode("Six Feet Under", "03", "09")
  val Episode4 =Episode("Sopranos", "04", "01")

  val FolderContents = Map(
    "Six Feet Under" -> Folder(
      "Six Feet Under",
      Map(
        "02" -> Folder("02"),
        "03" -> Folder("03")
      )
    ),
    "Sopranos" -> Folder(
      "Sopranos",
      Map(
        "04" -> Folder("04")
      )
    )
  )

}
