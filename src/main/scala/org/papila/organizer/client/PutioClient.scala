package org.papila.organizer.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import org.papila.organizer.client.PutioClient.FileType.FileType
import spray.json.{DefaultJsonProtocol, JsonFormat, RootJsonFormat}
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

trait PutioClient {

  import PutioClient._
  import PutioJsonSupport._

  val token: AccessToken
  val url = Uri(s"https://api.put.io/v2/files/list")
  val tokenTuple = ("oauth_token", token)

  def listFiles(f: FolderId, t: FileType, perPage: String)
               (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): FileListResponse = {

    val uri = url
      .withQuery(Query(tokenTuple, ("fileType", t.toString), ("parent_id", f), ("per_page", perPage)))

    println(s"Listfiles: $uri")

    val eventualRes = Http().singleRequest(HttpRequest(method = HttpMethods.GET, uri = uri))

    val rs = for {res <- eventualRes
                  fileListRes <- res match {
                    case HttpResponse(StatusCodes.OK, _, e, _) => Unmarshal(e).to[FileListResponse]
                    case x => throw new Exception(s"list files failed: ${x.status} ${x.httpMessage}")
                  }
    } yield fileListRes

    Await.result(rs, 10 seconds)
  }

  def createFolder(f: FolderId, parentId: FileId)
                  (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): CreateFolderResponse = {
    val uri = Uri("https://api.put.io/v2/files/create-folder")
      .withQuery(Query(tokenTuple))

    val eventualRes = Http().singleRequest(HttpRequest(method = HttpMethods.POST, uri = uri))

    val rs = for {res <- eventualRes
                  createFolderRes <- res match {
                    case HttpResponse(StatusCodes.OK, _, e, _) => Unmarshal(e).to[CreateFolderResponse]
                    case x => throw new Exception(s"Create folder failed: ${x.status} ${x.httpMessage}")
                  }
    } yield createFolderRes

    Await.result(rs, 10 seconds)
  }



}

object PutioClient {
  type FolderId = String

  object FileType extends Enumeration {
    type FileType = Value
    val Folder = Value("FOLDER")
    val Video = Value("VIDEO")
  }

  type FileId = Int
  type FileName = String
  type AccessToken = String

  case class File(id: FileId, name: FileName, parent_id: FileId)

  case class FileListResponse(
                               files: List[File],
                               parent: File,
                               cursor: Option[String]
                             )

  case class CreateFolderResponse(file: File)

  object PutioJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val fileFormat: JsonFormat[File] = jsonFormat3(File)
    implicit val fileListResFormat: RootJsonFormat[FileListResponse] = jsonFormat3(FileListResponse)
    implicit val createFolderResFormat: RootJsonFormat[CreateFolderResponse] = jsonFormat1(CreateFolderResponse)
  }

}

