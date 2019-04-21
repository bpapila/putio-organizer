package org.papila.organizer

import org.papila.organizer.client.PutioClient.FileName

object StringUtils {

  def extractName(fileName: FileName): (String, String, String) = {
    val pattern = """(?i)(.+)S(\d{2})E(\d{2}).*""".r

    val pattern(series, season, episode) = fileName

    return (
      formatSeriesName(series),
      season,
      episode
    )
  }

  def formatSeriesName(series: String) = {
    series.replaceAll("[\\._-]", " ").split(' ').map(_.capitalize).mkString(" ").trim
  }
}