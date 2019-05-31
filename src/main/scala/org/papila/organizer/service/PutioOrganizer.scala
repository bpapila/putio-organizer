package org.papila.organizer.service

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import org.papila.organizer.client.PutioClient.{File, FileName}
import org.papila.organizer.service.Organizer._
import org.papila.organizer.service.StringUtils.extractSeriesName

import scala.concurrent.ExecutionContext
import scala.util.Success

trait PutioOrganizer {

  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val ec: ExecutionContext

  def filesSource(bufferSize: Int = 100): Source[File, SourceQueueWithComplete[File]] =
    Source.queue[File](100, OverflowStrategy.dropBuffer)

  def fetchFolderFilesRecFlow(queue: SourceQueueWithComplete[File], putIoService: PutIoService) = Flow[File].collect {
    case folder@File(folderId, _, _, "FOLDER") =>
      putIoService.offerFilesUnderDir(folderId, queue)
      folder
    case f@File(_, _, _, "VIDEO") =>
      f
  }

  def videoFileFilter = Flow[File].collect {
    case f@File(_, _, _, "VIDEO") => f
  }

  def fileNameExtractor: Flow[File, (Episode, File), NotUsed] =
    Flow[File].map { f =>
      (extractSeriesName(f.name), f)
    }

  def videoFinderRecursive(
                            putioService: PutIoService,
                            bufferSize: Int = 100
                          ): (SourceQueueWithComplete[File], Source[EpisodeWithFile, NotUsed]) = {

    filesSource(bufferSize).preMaterialize() match {
      case (queue, source) =>
        (
          queue,
          source
            .log("SOURCE", x => println(s"SOURCE:     $x"))
            .via(fetchFolderFilesRecFlow(queue, putioService))
            .via(videoFileFilter)
            .via(fileNameExtractor)
            .log("EXTRACTOR", x => println(s"EXTRACTOR:     $x"))
        )
    }
  }

  def organizeFoldersFlow(dict: Map[FileName, Organizer.Series]): Flow[EpisodeWithFile, FolderContents, NotUsed] =
//    Flow[EpisodeWithFile].fold(Map.empty[String, Folder])()
  ???


  def foo(fs: Map[String, Folder], ef: EpisodeWithFile): Map[String, Folder] = {
    val fsWithSeries = create(fs, ef._1.series)
    val fsWithSeason = create(fsWithSeries ++ fsWithSeries(ef._1.series).items, ef._1.season)
    println(a)
    a
  }

  def create(fs: FolderContents, key: String): Map[String, Folder] = {
    fs get key match {
      case None => fs + (key -> Folder(key))
      case Some(_) => fs
    }
  }


  //    Flow[EpisodeWithFile].statefulMapConcat { () =>
  //      var fileSystem = Map.empty[String, Folder]
  //      ef => {
  //        val episode = ef._1
  //        fileSystem.get(episode.series) match {
  //          case None => fileSystem += (episode.series -> Folder(episode.series))
  //          case Some(s) =>
  //        }
  //
  //
  //      }

  //        fileSystem
  //      List.empty
  //    }


}