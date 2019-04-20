package org.papila.organizer

import org.scalatest.FlatSpec

import org.scalatest.Matchers._

class OrganizerAppTest extends FlatSpec {


  import OrganizerApp._

  behavior of "extract name"

  it should "extract series, season and episode" in {
    extractName("The.Walking.Dead.S09E15.The.Calm.Before.REPACK.1080p.AMZN.WEB-DL.DD+5.1.H.264-CasStudio.mkv") shouldBe ("The Walking Dead","09", "15")
    extractName("Its.Always.Sunny.in.Philadelphia.S13E10.1080p.WEB.H264-METCON.mkv") shouldBe ("Its Always Sunny In Philadelphia","13", "10")
    extractName("Its_Always_Sunny_in_Philadelphia_S13E10.1080p.WEB.H264-METCON.mkv") shouldBe ("Its Always Sunny In Philadelphia","13", "10")
    extractName("better.things.s03e07.1080p.web.h264-memento.mkv") shouldBe ("Better Things","03", "07")
  }
}
