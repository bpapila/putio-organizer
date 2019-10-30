package org.papila.organizer.service

import org.papila.organizer.client.PutioClient.{FileName, PutIoFile}
import org.papila.organizer.service.Organizer.Episode

object FileNameParser {

  def fileToEpisode(file: PutIoFile): Episode = {
    val pattern = """(?i)(.+)S(\d{2}) ?E(\d{2}).*""".r

    val pattern(series, season, episode) = file.name

    Episode(
      formatFileName(series),
      season,
      episode,
      file
    )
  }

  case class Movie(name: String, year: String)

  def fileToMovie(file: PutIoFile): Movie = {
    val pattern = """(.+)(19\d{2}|20\d{2}).*""".r

    val pattern(name, year) = file.name

    Movie(formatFileName(name), year)
  }

  def formatFileName(series: String): FileName = {
    series.replaceAll("[\\._-]", " ").split(' ').map(_.capitalize).mkString(" ").trim
  }
}
