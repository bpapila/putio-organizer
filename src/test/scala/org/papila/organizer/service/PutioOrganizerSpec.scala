package org.papila.organizer.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source, SourceQueueWithComplete}
import org.mockito.Mockito.{never, reset, times, verify, _}
import org.papila.organizer.GraphPutio
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{CreateFolderResponse, FileType, PutIoFile}
import org.papila.organizer.service.Organizer.{Episode, File, FilesMap, Folder}
import org.scalatest.Matchers._
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{BeforeAndAfter, FlatSpec}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class PutioOrganizerSpec extends FlatSpec with BeforeAndAfter {

  import Fixtures._

  implicit val systemImpl = ActorSystem("TestSystem")
  implicit val materializerImpl = ActorMaterializer()
  implicit val ecImpl = systemImpl.dispatcher

  val putioClient = mock[PutioClient]
  val queue = mock[SourceQueueWithComplete[PutIoFile]]
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

    val (queue, f) = source.take(2).toMat(Sink.seq[PutIoFile])(Keep.both).run()
    queue.offer(Folder1)
    queue.offer(Folder2)

    Await.result(f, 3 seconds) shouldBe Seq(Folder1, Folder2)
  }

  behavior of "fetchFolderFilesRecFlow"

  it should "pass on "

  it should "call offerFilesUnderDir on folders" in {

    val flowUnderTest = putioOrganizer.fetchFolderFilesRecFlow(queue, putIoService)

    val f = Source[PutIoFile](List(Folder1, Folder2, File1))
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

    val f = Source[PutIoFile](List(Folder1, Folder2, File1))
      .via(flowUnderTest)
      .toMat(Sink.seq)(Keep.right)
      .run()

    val seq = Await.result(f, 3 seconds)
    seq shouldBe Seq(Folder1, Folder2, File1)
  }

  "videoFileFilter" should "filter only video files" in {
    val flowUnderTest = putioOrganizer.videoFileFilter

    val f = Source[PutIoFile](List(Folder1, Folder2, File1))
      .via(flowUnderTest)
      .toMat(Sink.seq)(Keep.right)
      .run()

    val seq = Await.result(f, 3 seconds)
    seq shouldBe Seq(File1)
  }

  "createInFolder" should "create key under FolderContents" in {
    val fc: FilesMap = Map.empty[String, File]
    GraphPutio.createFileEntry(fc, "test")("test") shouldBe File("test")
  }

  "createInFolder" should "not duplicate item when key already exist" in {
    val fc: FilesMap = Map.empty[String, File]
    fc + ("test" -> File("test"))

    val fcUpdated = GraphPutio.createFileEntry(fc, "test")
    fcUpdated.size shouldBe 1
    fcUpdated("test") shouldBe File("test")
  }

  "organizeSeriesIntoFolder" should "create subfolder" in {
    val fc: FilesMap = Map.empty[String, File]

    val episode = Episode("Six Feet Under", "2", "1", mock[PutIoFile])

    val updatedFc = GraphPutio.organizeSeriesIntoFolder(fc, episode)

    updatedFc(episode.series) shouldBe
      File(episode.series, Map(episode.seasonNo -> File(episode.seasonNo)))
  }

  "organizeFoldersFlow" should "organize files into folder structure" in {

    val testList = List(SixFeetUnderEpisode1, SixFeetUnderEpisode2, SixFeetUnderEpisode3, SopranosEpisode)

    val res = Source(testList)
      .via(GraphPutio.organizeFoldersFlow())
      .toMat(Sink.head)(Keep.right)
      .run()

    val result = Await.result(res, 3 seconds)
    result shouldBe FolderContents

  }

  "folderCreatorFlow" should "create missing series and season folders" in {
    val rootFolder = Folder("TV Series", 1)

    when(putioClient.createFolder("Six Feet Under", 1))
      .thenReturn(CreateFolderResponse(PutIoFile(10, "Six Feet Under", 1)))
    when(putioClient.createFolder("Season 02", 10))
      .thenReturn(CreateFolderResponse(PutIoFile(100, "Season 02", 10)))

    val f = Source(List(SixFeetUnderEpisode1))
      .via(GraphPutio.folderCreatorFlow(rootFolder, putioClient)).take(1).toMat(Sink.ignore)(Keep.right).run()

    Await.result(f, 5 seconds)

    verify(putioClient, times(1)).createFolder("Six Feet Under", 1)
    verify(putioClient, times(1)).createFolder("Season 02", 10)
  }

  "folderCreatorFlow" should "create missing season folder when series already exists" in {
    val rootFolder = Folder("TV Series", 1, Map("Six Feet Under" -> Folder("Six Feet Under", 10)))

    when(putioClient.createFolder("Season 02", 10))
      .thenReturn(CreateFolderResponse(PutIoFile(100, "Season 02", 10)))

    val f = Source(List(SixFeetUnderEpisode1))
      .via(GraphPutio.folderCreatorFlow(rootFolder, putioClient)).take(1).toMat(Sink.ignore)(Keep.right).run()

    Await.result(f, 5 seconds)

    verify(putioClient, times(1)).createFolder("Season 02", 10)
  }

  "folderCreatorFlow" should "not create folder when folders exists" in {
    val f = Source(List(SixFeetUnderEpisode1))
      .via(GraphPutio.folderCreatorFlow(RootFolder, putioClient)).take(1).toMat(Sink.ignore)(Keep.right).run()

    Await.result(f, 5 seconds)
  }

  "folderCreatorFlow" should "only create folder once" in {
    val rootFolder = Folder("TV Series", 1, Map("Six Feet Under" -> Folder("Six Feet Under", 10)))

    when(putioClient.createFolder("Season 02", 10))
      .thenReturn(CreateFolderResponse(PutIoFile(100, "Season 02", 10)))

    val episodeList = List(SixFeetUnderEpisode1, SixFeetUnderEpisode2)

    val f = Source(episodeList)
      .via(GraphPutio.folderCreatorFlow(rootFolder, putioClient))
      .take(episodeList.size)
      .toMat(Sink.ignore)(Keep.right).run()

    Await.result(f, 5 seconds)

    verify(putioClient, times(1)).createFolder("Season 02", 10)
  }

  "folderCreatorFlow" should "send episode downstream" in {
    val f = Source(List(SixFeetUnderEpisode1))
      .via(GraphPutio.folderCreatorFlow(RootFolder, putioClient)).take(1).toMat(Sink.seq)(Keep.right).run()

    Await.result(f, 5 seconds) shouldBe Seq(SixFeetUnderEpisode1)
  }

}

object Fixtures {

  val SeasonFolder = Folder("Season 02", 100)
  val SeriesFolder = Folder("Six Feet Under", 10, Map(SeasonFolder.name -> SeasonFolder))
  val RootFolder = Folder("TV Series", 1, Map("Six Feet Under" -> SeriesFolder))

  val Folder1 = PutIoFile(123123, "Folder1", 123, FileType.Folder.toString)
  val Folder2 = PutIoFile(123456, "Folder2", 123, FileType.Folder.toString)
  val File1 = PutIoFile(123789, "File1", 123456, FileType.Video.toString)

  val SixFeetUnderEpisode1 = Episode("Six Feet Under", "02", "01", mock[PutIoFile])
  val SixFeetUnderEpisode2 = Episode("Six Feet Under", "02", "02", mock[PutIoFile])
  val SixFeetUnderEpisode3 = Episode("Six Feet Under", "03", "09", mock[PutIoFile])
  val SopranosEpisode = Episode("Sopranos", "04", "01", mock[PutIoFile])

  val FolderContents = Map(
    "Six Feet Under" -> File(
      "Six Feet Under",
      Map(
        "02" -> File("02"),
        "03" -> File("03")
      )
    ),
    "Sopranos" -> File(
      "Sopranos",
      Map(
        "04" -> File("04")
      )
    )
  )

}
