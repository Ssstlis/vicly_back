package services

import java.io.File
import java.nio.file.Paths

import akka.stream.scaladsl.{FileIO, Source}
import com.google.inject.{Inject, Singleton}
import daos.AttachmentDao
import models.SeaweedResponse
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
                                   userService: UserService,
                                   ws: WSClient
                                 )(implicit ec: ExecutionContext) {

  val path = config.get[String]("path.upload")


  //  def postFile(wsClient: StandaloneWSClient) = {
  //    import play.api.mvc.MultipartFormData.FilePart
  //    val io = FileIO.fromPath(Paths.get("/home/spoofer/nginx-1.12.0.tar.gz"))
  //    val f = FilePart("uploadTransfer", "nginx-1.12.0.tar.gz",
  //      Some("application/octet-stream"), io)
  //    val s = Source.single(f)
  //
  //    wsClient.url("http://localhost:9001/uploadTransfer".post(s)
  //  }

  val seaweedfs_url="http://80.241.209.42"

  def saveFileNew(file: File, originalFilename: String, userId: Int, isAvatar: Boolean) = {
    val filePart = MultipartFormData.FilePart("file", originalFilename, None, FileIO.fromPath(file.toPath))
    val dataPart = DataPart("key", "value")



    ws.url(seaweedfs_url+":9333/submit")
      .withRequestTimeout(30.seconds)
      .post(Source(filePart :: dataPart :: Nil))
      .map { response =>
        response.json.asOpt(SeaweedResponse.reads()).flatMap { seaweedResponse =>
           attachmentDao.saveFile(seaweedResponse.fileId, seaweedResponse.fileName, userId, seaweedResponse.fileSize, isAvatar)
              .map{ attachment =>
                // TODO old avatar file deleting
                userService.setAvatar(userId, attachment._id.toString)
                attachment
              }

        }
      }
      .recover { case ex =>
        Logger("application").error(ex.getLocalizedMessage, ex)
        None
      }
  }

  def getFile(id: String) = {
    attachmentDao.find(id)
      .collect { case attachment =>
        val url = s"${seaweedfs_url}:8080/${attachment.fid}}"
        ws.url(url)
          .withMethod("GET")
          .stream()
          .map { response =>
            if (response.status < 300 && response.status >= 200)
              Some(response.bodyAsSource)
            else
              None
          }
      }
  }

  def getFileAvatar(id: String, width: Option[Int]) = {
    attachmentDao.find(id)
      .collect { case attachment =>
        val url = s"${seaweedfs_url}:8080/${attachment.fid}${if (width.isDefined) "?width=" + width.get  else ""}"
        println(url)
        ws.url(url)
          .withMethod("GET")
          .stream()
          .map { response =>
            if (response.status < 300 && response.status >= 200)
              Some(response.bodyAsSource)
            else
              None
          }
      }
  }

  def findByUserId(userId: Int) = {
    attachmentDao.findByUserId(userId)
  }

  //  def remove(userId: Int, uuid: String, path: String) = attachmentDao.remove(userId, uuid, path)
}
