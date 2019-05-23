package org.papila.organizer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{AccessToken, FileType}
import org.papila.organizer.service.{Organizer, PutioOrganizer}
import scala.concurrent.duration._
import scala.concurrent.Await

object GraphPutio extends App {

  implicit val system = ActorSystem("TestSystem")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val putioClient = new PutioClient {
    override lazy val token: AccessToken = "VTQWG4M3LK5I5LD7IL25"
  }

//  val (queue, source) = filesSource(10)
//    .preMaterialize()
//
//  val sinkPrint = Sink.foreach(println)
//
//  //  source
//  //    .take(10)
//  //    .via(fetchFolderFilesFlow(queue))
//  //    .via(videoFileFilter)
//  //    .via(fileNameExtractor
//  //    .log("EXTRACTOR", x => println(s"EXTRACTOR:     $x"))
//  //    .to(Sink.ignore)
//  //    .run()
//
//  val (queue, source) = videoFinderRecursive()
//
//  source.to(sinkPrint).run()
//  putioClient
//    .listFiles(Organizer.TvShowFolderId, None, "2")
//    .files
//    .foreach(file => {
//      val a = Await.result(queue.offer(file), 5 seconds)
//      println(s"OfferResult: $a")
//      println(file)
//      println()
//    })
}
