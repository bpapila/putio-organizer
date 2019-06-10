package org.papila.organizer.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.SourceQueueWithComplete
import org.papila.organizer.client.PutioClient
import org.papila.organizer.client.PutioClient.{PutIoFile, FileId}

import scala.concurrent.ExecutionContext

class PutIoService(client: PutioClient)
                  (implicit val system: ActorSystem,
                   mat: ActorMaterializer,
                   ec: ExecutionContext) {
  def offerFilesUnderDir(id: FileId, srcQueue: SourceQueueWithComplete[PutIoFile]): Unit =
    client.listFiles(id, None, "10").files
      .foreach(srcQueue.offer)



}
