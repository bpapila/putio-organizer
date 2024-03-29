package org.papila.organizer.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import org.papila.organizer.client.PutioClient.AccessToken
import org.papila.organizer.client.PutioClient.FileType.FileType
import spray.json.{DefaultJsonProtocol, JsonFormat, RootJsonFormat}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class PutioClient(val token: AccessToken)
                 (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer) {

  import PutioClient._
  import PutioJsonSupport._

  val url = Uri(s"https://api.put.io/v2/files/list")
  val tokenTuple = ("oauth_token", token)

  def listFolders(f: FolderId): FileListResponse = listFiles(f, Some(FileType.Folder))

  def listFiles(f: FolderId, t: Option[FileType], perPage: String = "999"): FileListResponse = {

    val query = t match {
      case Some(fileType) => Query(tokenTuple, ("file_type", fileType.toString), ("parent_id", f.toString), ("per_page", perPage))
      case None => Query(tokenTuple, ("parent_id", f.toString), ("per_page", perPage))
    }

    val uri = url.withQuery(query)

    val eventualRes = Http().singleRequest(HttpRequest(method = HttpMethods.GET, uri = uri))

    val rs = for {res <- eventualRes
                  fileListRes <- res match {
                    case HttpResponse(StatusCodes.OK, _, e, _) => Unmarshal(e).to[FileListResponse]
                    case x => throw new Exception(s"list files failed: ${x.status} ${x.httpMessage}")
                  }
    } yield fileListRes

    Await.result(rs, 10 seconds)
  }

  def createFolder(name: String, parentId: FileId): CreateFolderResponse = {
    println(s"Creating folder $name on parent $parentId")

    val body = FormData(("name", name.toString), ("parent_id", parentId.toString))

    val res = Marshal(body).to[RequestEntity].flatMap { entity =>
      val uri = Uri("https://api.put.io/v2/files/create-folder")
        .withQuery(Query(tokenTuple))

      val eventualRes = Http().singleRequest(HttpRequest(method = HttpMethods.POST, uri = uri, entity = entity))

      for {res <- eventualRes
           createFolderRes <- res match {
             case HttpResponse(StatusCodes.OK, _, e, _) => Unmarshal(e).to[CreateFolderResponse]
             case x => throw new Exception(s"Create folder failed: ${x.status} ${x.httpMessage}")
           }
      } yield createFolderRes
    }

    Await.result(res, 10 seconds)
  }

  def moveFile(f: FileId, target: FileId): PutIoFile = {
    println(s"Moving file $f to $target")
    val uri = Uri("https://api.put.io/v2/files/move").withQuery(Query(tokenTuple))
    val body = FormData(("file_ids", f.toString), ("parent_id", target.toString))

    Await.result(
      Marshal(body).to[RequestEntity].map { entity =>
        val eventualRes = Http().singleRequest(HttpRequest(method = HttpMethods.POST, uri = uri, entity = entity))
        Await.result(eventualRes, 10 seconds) match {
          case HttpResponse(StatusCodes.OK, _, e, _) => PutIoFile(1, "", 1) // TODO: dummy data to compile
          case x => throw new Exception(s"Move file failed: ${x.status} ${x.httpMessage}")
        }
      }
      , 5 seconds)
  }
}

object PutioClient {

  object FileType extends Enumeration {
    type FileType = Value
    val Folder = Value("FOLDER")
    val Video = Value("VIDEO")
  }

  type FileId = Int
  type FolderId = Int
  type FileName = String
  type AccessToken = String

  case class PutIoFile(id: FileId, name: FileName, parent_id: FileId, file_type: String = "")

  case class FileListResponse(
                               files: List[PutIoFile],
                               parent: PutIoFile,
                               cursor: Option[String]
                             )

  case class CreateFolderResponse(file: PutIoFile)

  object PutioJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val fileFormat: JsonFormat[PutIoFile] = jsonFormat4(PutIoFile)
    implicit val fileListResFormat: RootJsonFormat[FileListResponse] = jsonFormat3(FileListResponse)
    implicit val createFolderResFormat: RootJsonFormat[CreateFolderResponse] = jsonFormat1(CreateFolderResponse)
  }

}

