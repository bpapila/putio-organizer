package org.papila.organizer.service

import org.papila.organizer.client.PutioClient.{FileName, PutIoFile}
import org.papila.organizer.service.Organizer.Episode

object StringUtils {

  def fileToEpisode(file: PutIoFile): Episode = {
    val pattern = """(?i)(.+)S(\d{2}) ?E(\d{2}).*""".r

    val pattern(series, season, episode) = file.name

    return Episode(
      formatSeriesName(series),
      season,
      episode,
      file
    )
  }

  def formatSeriesName(series: String): FileName = {
    series.replaceAll("[\\._-]", " ").split(' ').map(_.capitalize).mkString(" ").trim
  }
}
