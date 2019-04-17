package services

import java.io.File
import java.nio.file.Paths

import akka.stream.scaladsl.{FileIO, Source}
import com.google.inject.{Inject, Singleton}
import daos.AttachmentDao
import play.api.{Configuration, Logger}
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData._

import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import scala.sys.process._
import scala.util.Try
import scala.concurrent.duration._

@Singleton
class AttachmentService @Inject()(
 attachmentDao: AttachmentDao,
 config: Configuration,
 ws: WSClient
)(implicit ec: ExecutionContext) {

  val path = config.get[String]("path.upload")

  def saveFile(from: String, path: String, filename: String, userId: Int, size: Long) = {
    attachmentDao.saveFile(from, path, filename, userId, size)
  }

//  def postFile(wsClient: StandaloneWSClient) = {
//    import play.api.mvc.MultipartFormData.FilePart
//    val io = FileIO.fromPath(Paths.get("/home/spoofer/nginx-1.12.0.tar.gz"))
//    val f = FilePart("uploadTransfer", "nginx-1.12.0.tar.gz",
//      Some("application/octet-stream"), io)
//    val s = Source.single(f)
//
//    wsClient.url("http://localhost:9001/uploadTransfer".post(s)
//  }

  def saveFileNew(userId: Int, file: File) = {
    val filePart = MultipartFormData.FilePart("hello", "lol.png", Some("text/plain"), FileIO.fromPath(file.toPath))
    val dataPart = DataPart("key", "value")

    ws.url("http://localhost:9333/submit")
      .withRequestTimeout(30.seconds)
      .post(Source(filePart :: dataPart :: Nil))
      .map(response => response.status < 300 && response.status >= 200)
      .recover { case ex =>
        Logger("application").error(ex.getLocalizedMessage, ex)
        false
      }
  }

  def find(uuid: String, groupIdO: Option[Int]) = {
    for {
      groupId <- groupIdO

      attach <- attachmentDao.find(uuid)
      filePath = s"$path/$groupId/$uuid"
      output <- Try(Seq("sh", "-c", s"ls $filePath -1") !!).toOption
      fileName <- output.split("\\n").headOption if fileName == attach.filename
      file <- Try(new File(s"$filePath/$fileName")).toOption
    } yield file
  }

  def findByUserId(userId: Int) = {
    attachmentDao.findByUserId(userId)
  }

  def remove(userId: Int, uuid: String, path: String) = attachmentDao.remove(userId, uuid, path)
}
