package org.papila.organizer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.AccessToken
import org.papila.organizer.service.{Organizer, PutioOrganizer, PutioService}

import scala.concurrent.Await
import scala.concurrent.duration._

object GraphPutio extends App with PutioOrganizer {

  implicit val system = ActorSystem("TestSystem")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val putioClient = new PutioClient {
    override lazy val token: AccessToken = "VTQWG4M3LK5I5LD7IL25"
  }

  val putioService = new PutioService(putioClient)

  val (queue, src) = videoFinderRecursive(putioService)
  val g = src.toMat(Sink.ignore)(Keep.right).run()

  putioClient
    .listFiles(Organizer.TvShowFolderId, None, "2")
    .files
    .foreach(queue.offer)

  Await.result(g, 180 seconds)
}
