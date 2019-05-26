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

import scala.concurrent.duration._
import org.scalatest.Matchers._

import scala.concurrent.{Await, ExecutionContext, Future}

class PutioOrganizerSpec extends FlatSpec with BeforeAndAfter{

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

}
