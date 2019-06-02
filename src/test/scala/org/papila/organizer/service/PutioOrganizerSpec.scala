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

  val folder1 = File(123123, "Folder1", 123, FileType.Folder.toString)
  val folder2 = File(123456, "Folder2", 123, FileType.Folder.toString)
  val file1 = File(123789, "File1", 123456, FileType.Video.toString)

  "filesSource" should "return queue source" in {
    val source = putioOrganizer.filesSource(100)

    val (queue, f) = source.take(2).toMat(Sink.seq[File])(Keep.both).run()
    queue.offer(folder1)
    queue.offer(folder2)

    Await.result(f, 3 seconds) shouldBe Seq(folder1, folder2)
  }

  behavior of "fetchFolderFilesRecFlow"

  it should "pass on "

  it should "call offerFilesUnderDir on folders" in {

    val flowUnderTest = putioOrganizer.fetchFolderFilesRecFlow(queue, putIoService)

    val f = Source[File](List(folder1, folder2, file1))
      .via(flowUnderTest)
      .toMat(Sink.seq)(Keep.right)
      .run()

    Await.result(f, 3 seconds)

    verify(putIoService, times(1))
      .offerFilesUnderDir(folder1.id, queue)
    verify(putIoService, times(1))
      .offerFilesUnderDir(folder2.id, queue)
    verify(putIoService, never())
      .offerFilesUnderDir(file1.id, queue)
  }

  it should "pass on files and folders" in {

    val flowUnderTest = putioOrganizer.fetchFolderFilesRecFlow(queue, putIoService)

    val f = Source[File](List(folder1, folder2, file1))
      .via(flowUnderTest)
      .toMat(Sink.seq)(Keep.right)
      .run()

    val seq = Await.result(f, 3 seconds)
    seq shouldBe Seq(folder1, folder2, file1)
  }

  "videoFileFilter" should "filter only video files" in {
    val flowUnderTest = putioOrganizer.videoFileFilter

    val f = Source[File](List(folder1, folder2, file1))
      .via(flowUnderTest)
      .toMat(Sink.seq)(Keep.right)
      .run()

    val seq = Await.result(f, 3 seconds)
    seq shouldBe Seq(file1)
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
      (Episode("Six Feet Under", "02", "01"), mock[File]),
      (Episode("Six Feet Under", "02", "02"), mock[File]),
      (Episode("Six Feet Under", "03", "09"), mock[File]),
      (Episode("Sopranos", "04", "01"), mock[File])
    )

    val expectedFolder = Map(
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

    val res = Source(testList)
      .via(GraphPutio.organizeFoldersFlow())
      .toMat(Sink.head)(Keep.right)
      .run()

    val result = Await.result(res, 3 seconds)
    result shouldBe expectedFolder

  }


}
