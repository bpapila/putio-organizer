package org.papila.organizer.service

import org.papila.organizer.client.PutioClient.{FileName, PutIoFile}
import org.papila.organizer.service.Organizer.Episode

object FileNameParser {

  def fileToEpisode(file: PutIoFile): Episode = {
    val (series, season, episode) = extractEpisode(file)

    Episode(
      extractMovie(series),
      season,
      episode,
      file
    )
  }

  def nameParsable(file: PutIoFile): Boolean = {
    val (series, season, episode) = extractEpisode(file)

    if (series != null || season != null || episode != null) {
      true
    } else {
      false
    }
  }

  private def extractEpisode(file: PutIoFile) = {
    val pattern = """(?i)(.+)S(\d{2}) ?E(\d{2}).*""".r

    val pattern(series, season, episode) = file.name
    (series, season, episode)
  }

  case class Movie(name: String, year: String)

  def fileToMovie(file: PutIoFile): Movie = {
    val pattern = """(.+)(19\d{2}|20\d{2}).*""".r

    val pattern(name, year) = file.name

    Movie(extractMovie(name), year)
  }

  def extractMovie(series: String): FileName = {
    series.replaceAll("[\\._-]", " ").split(' ').map(_.capitalize).mkString(" ").trim
  }
}
