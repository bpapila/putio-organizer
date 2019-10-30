package org.papila.organizer

import org.papila.organizer.client.PutioClient.PutIoFile
import org.papila.organizer.service.Organizer.Episode
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class FileNameParserTest extends FlatSpec {
  import org.papila.organizer.service.FileNameParser._
  import StringUtilsTestFixtures._

  "extract name" should "extract Episode(series, season and episode, fileId)" in {
    fileToEpisode(seriesFile1) shouldBe Episode("The Walking Dead","09", "15", seriesFile1)
    fileToEpisode(seriesFile2) shouldBe Episode("Its Always Sunny In Philadelphia","13", "10", seriesFile2)
    fileToEpisode(seriesFile3) shouldBe Episode("Its Always Sunny In Philadelphia","13", "10", seriesFile3)
    fileToEpisode(seriesFile4) shouldBe Episode("Better Things","03", "07", seriesFile4)
    fileToEpisode(seriesFile5) shouldBe Episode("The Expanse","01", "01", seriesFile5)
  }

  "formatSeriesName" should "format series name normalizing it" in {
    formatFileName("The.Walking.Dead") shouldBe "The Walking Dead"
    formatFileName("Its.Always.Sunny.in.Philadelphia") shouldBe "Its Always Sunny In Philadelphia"
    formatFileName("     Its.Always.Sunny.in.Philadelphia") shouldBe "Its Always Sunny In Philadelphia"
    formatFileName("better_things   ") shouldBe "Better Things"
    formatFileName("The Expanse - ") shouldBe "The Expanse"
  }

  "fileToMovie" should "extract Movie(name, year, quality)" in {
    fileToMovie(movieFile1) shouldBe Movie("Ant Man And The Wasp", "2018")
    fileToMovie(movieFile2) shouldBe Movie("John Wick 3", "2019")
    fileToMovie(movieFile3) shouldBe Movie("Triple Frontier", "2019")
  }

}

object StringUtilsTestFixtures {
  val seriesFile1 = PutIoFile(100, "The.Walking.Dead.S09E15.The.Calm.Before.REPACK.1080p.AMZN.WEB-DL.DD+5.1.H.264-CasStudio.mkv", 0)
  val seriesFile2 = PutIoFile(100, "Its.Always.Sunny.in.Philadelphia.S13E10.1080p.WEB.H264-METCON.mkv", 0)
  val seriesFile3 = PutIoFile(100, "Its_Always_Sunny_in_Philadelphia_S13E10.1080p.WEB.H264-METCON.mkv", 0)
  val seriesFile4 = PutIoFile(100, "better.things.s03e07.1080p.web.h264-memento.mkv", 0)
  val seriesFile5 = PutIoFile(100, "The Expanse - S01 E01 - Dulcinea (720p HDTV).mp4", 0)

  val movieFile1 = PutIoFile(111, "Ant-Man.And.The.Wasp.2018.1080p.BluRay.x264-[YTS.AM].mp4", 0)
  val movieFile2 = PutIoFile(112, "John.Wick.3.2019.HDRip.XviD.AC3-EVO.avi", 0)
  val movieFile3 = PutIoFile(113, "Triple.Frontier.2019.720p.WEBRip.x264-[YTS.AM].mp4", 0)

}
