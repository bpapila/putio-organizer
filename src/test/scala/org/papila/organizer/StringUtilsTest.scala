package org.papila.organizer

import org.papila.organizer.client.PutioClient.PutIoFile
import org.papila.organizer.service.Organizer.Episode
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class StringUtilsTest extends FlatSpec {
  import org.papila.organizer.service.StringUtils._
  import StringUtilsTestFixtures._

  "extract name" should "extract series, season and episode" in {
    fileToEpisode(file1) shouldBe Episode("The Walking Dead","09", "15", file1)
    fileToEpisode(file2) shouldBe Episode("Its Always Sunny In Philadelphia","13", "10", file2)
    fileToEpisode(file3) shouldBe Episode("Its Always Sunny In Philadelphia","13", "10", file3)
    fileToEpisode(file4) shouldBe Episode("Better Things","03", "07", file4)
    fileToEpisode(file5) shouldBe Episode("The Expanse","01", "01", file5)
  }

  "formatSeriesName" should "format series name" in {
    formatSeriesName("The.Walking.Dead") shouldBe "The Walking Dead"
    formatSeriesName("Its.Always.Sunny.in.Philadelphia") shouldBe "Its Always Sunny In Philadelphia"
    formatSeriesName("     Its.Always.Sunny.in.Philadelphia") shouldBe "Its Always Sunny In Philadelphia"
    formatSeriesName("better_things   ") shouldBe "Better Things"
    formatSeriesName("The Expanse - ") shouldBe "The Expanse"
  }



}

object StringUtilsTestFixtures {
  val file1 = PutIoFile(100, "The.Walking.Dead.S09E15.The.Calm.Before.REPACK.1080p.AMZN.WEB-DL.DD+5.1.H.264-CasStudio.mkv", 0)
  val file2 = PutIoFile(100, "Its.Always.Sunny.in.Philadelphia.S13E10.1080p.WEB.H264-METCON.mkv", 0)
  val file3 = PutIoFile(100, "Its_Always_Sunny_in_Philadelphia_S13E10.1080p.WEB.H264-METCON.mkv", 0)
  val file4 = PutIoFile(100, "better.things.s03e07.1080p.web.h264-memento.mkv", 0)
  val file5 = PutIoFile(100, "The Expanse - S01 E01 - Dulcinea (720p HDTV).mp4", 0)

}
