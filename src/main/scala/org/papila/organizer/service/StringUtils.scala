package org.papila.organizer.service

import org.papila.organizer.client.PutioClient.FileName
import org.papila.organizer.service.Organizer.Episode

object StringUtils {

  def extractSeriesName(fileName: FileName): Episode = {
    val pattern = """(?i)(.+)S(\d{2}) ?E(\d{2}).*""".r

    val pattern(series, season, episode) = fileName

    return Episode(
      formatSeriesName(series),
      season,
      episode
    )
  }

  def formatSeriesName(series: String): FileName = {
    series.replaceAll("[\\._-]", " ").split(' ').map(_.capitalize).mkString(" ").trim
  }
}
