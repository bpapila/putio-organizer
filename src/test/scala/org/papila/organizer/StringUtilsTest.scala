package org.papila.organizer

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class StringUtilsTest extends FlatSpec {
  import org.papila.organizer.service.StringUtils._

  "extract name" should "extract series, season and episode" in {
    extractName("The.Walking.Dead.S09E15.The.Calm.Before.REPACK.1080p.AMZN.WEB-DL.DD+5.1.H.264-CasStudio.mkv") shouldBe ("The Walking Dead","09", "15")
    extractName("Its.Always.Sunny.in.Philadelphia.S13E10.1080p.WEB.H264-METCON.mkv") shouldBe ("Its Always Sunny In Philadelphia","13", "10")
    extractName("Its_Always_Sunny_in_Philadelphia_S13E10.1080p.WEB.H264-METCON.mkv") shouldBe ("Its Always Sunny In Philadelphia","13", "10")
    extractName("better.things.s03e07.1080p.web.h264-memento.mkv") shouldBe ("Better Things","03", "07")
    extractName("The Expanse - S01 E01 - Dulcinea (720p HDTV).mp4") shouldBe ("The Expanse","01", "01")

  }

  "formatSeriesName" should "format series name" in {
    formatSeriesName("The.Walking.Dead") shouldBe "The Walking Dead"
    formatSeriesName("Its.Always.Sunny.in.Philadelphia") shouldBe "Its Always Sunny In Philadelphia"
    formatSeriesName("     Its.Always.Sunny.in.Philadelphia") shouldBe "Its Always Sunny In Philadelphia"
    formatSeriesName("better_things   ") shouldBe "Better Things"
    formatSeriesName("The Expanse - ") shouldBe "The Expanse"
  }
}
