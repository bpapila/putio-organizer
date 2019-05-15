package org.papila.organizer.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import org.papila.organizer.client.PutioClient.{File, FileType}
import org.scalatest.FlatSpec

import scala.concurrent.duration._
import org.scalatest.Matchers._

import scala.concurrent.{Await, ExecutionContext}

class PutioOrganizerSpec extends FlatSpec {

  implicit val systemImpl = ActorSystem("TestSystem")
  implicit val materializerImpl = ActorMaterializer()
  implicit val ecImpl = systemImpl.dispatcher

  var putioOrganizer = new PutioOrganizer {
    override implicit val system: ActorSystem = systemImpl
    override implicit val materializer: ActorMaterializer = materializerImpl
    override implicit val ec: ExecutionContext = ecImpl
  }

  val folder1 = File(123123, "Folder1", 123, FileType.Folder.toString)
  val folder2 = File(123456, "Folder2", 123, FileType.Folder.toString)

  "filesSource" should "return queue source" in {
    val source = putioOrganizer.filesSource()

    val (queue, f) = source.take(2).toMat(Sink.seq[File])(Keep.both).run()
    queue.offer(folder1)
    queue.offer(folder1)

    Await.result(f, 3 seconds) shouldBe Seq(folder1, folder2)
  }

  behavior of "fetchFolderFilesFlow"

  it should "pending" in {pending}


}
