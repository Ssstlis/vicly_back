package services

import java.io.File

import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.google.inject.{Inject, Singleton}
import daos.AttachmentDao
import models.{SeaweedResponse, User}
import org.bson.types.ObjectId
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData._
import play.api.{Configuration, Logger}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import cats.implicits._
import cats.data._

import scala.util.{Failure, Success}

@Singleton
class AttachmentService @Inject()(
  attachmentDao: AttachmentDao,
  config: Configuration,
  userService: UserService,
  ws: WSClient
)(implicit ec: ExecutionContext) {

  //  def postFile(wsClient: StandaloneWSClient) = {
  //    import play.api.mvc.MultipartFormData.FilePart
  //    val io = FileIO.fromPath(Paths.get("/home/spoofer/nginx-1.12.0.tar.gz"))
  //    val f = FilePart("uploadTransfer", "nginx-1.12.0.tar.gz",
  //      Some("application/octet-stream"), io)
  //    val s = Source.single(f)
  //
  //    wsClient.url("http://localhost:9001/uploadTransfer".post(s)
  //  }

  val seaweedfs_volume_url = config.get[String]("seaweed.address.volume")
  val seaweedfs_master_url = config.get[String]("seaweed.address.master")

  def saveFileNew(file: File, originalFilename: String, userId: Int, isAvatar: Boolean) = {
    val filePart = MultipartFormData.FilePart("file", originalFilename, None, FileIO.fromPath(file.toPath))
    val dataPart = DataPart("key", "value")

    ws.url(seaweedfs_volume_url + "/submit")
      .withRequestTimeout(30.seconds)
      .post(Source(filePart :: dataPart :: Nil))
      .map { response =>
        response.json.asOpt(SeaweedResponse.reads()).flatMap { seaweedResponse =>
          attachmentDao.saveFile(seaweedResponse.fileId, seaweedResponse.fileName, userId, seaweedResponse.fileSize, isAvatar)
            .map { attachment =>
              // TODO old avatar file deleting
              userService.setAvatar(userId, attachment._id)
              attachment
            }

        }
      }
      .recover { case ex =>
        Logger("application").error(ex.getLocalizedMessage, ex)
        None
      }
  }

  def saveFileAvatarNew(user: User, file: File, originalFilename: String, userId: Int) = {
    val filePart = MultipartFormData.FilePart("file", originalFilename, None, FileIO.fromPath(file.toPath))
    val dataPart = DataPart("key", "value")
    // TODO
    val deleteResult = user.avatar.flatMap { avatarId =>
      attachmentDao.findOneById(avatarId)
    }.map { attachment =>
      ws.url(s"$seaweedfs_master_url/${attachment.fid}")
        .delete()
        .map { _ =>
          if (attachmentDao.removeById(attachment._id).wasAcknowledged())
            Right("Was deleted!")
          else
            Left(new Exception("Error while deleting from BD"))
        }
        .recover { case ex =>
          Logger("application").error(ex.getLocalizedMessage, ex)
          Left(ex)
        }
    }.getOrElse(Future.successful(Right("Not needed deleting!")))

    deleteResult.flatMap {
      case Left(ex) => Future.successful(Left(ex))
      case Right(_) =>
        ws.url(seaweedfs_volume_url + "/submit")
          .withRequestTimeout(30.seconds)
          .post(Source(filePart :: dataPart :: Nil))
          .map { response =>
            for {
              seaweedResponse <- response.json.asOpt(SeaweedResponse.reads()).toRight(new Exception("Seaweedfs saving error"))
              attachment <- attachmentDao.saveFile(seaweedResponse.fileId, seaweedResponse.fileName, userId, seaweedResponse.fileSize, isAvatar = true)
                .toRight(new Exception("BD fid saving error,but seaweedfs saved successfully"))
            } yield {
              userService.setAvatar(userId, attachment._id).wasAcknowledged()
              attachment
            }
          }
          .recover { case ex =>
            Logger("application").error(ex.getLocalizedMessage, ex)
            Left(ex)
          }
    }

  }

  //    ws.url(seaweedfs_volume_url + "/submit")
  //      .withRequestTimeout(30.seconds)
  //      .post(Source(filePart :: dataPart :: Nil))
  //      .map { response =>
  //        response.json.asOpt(SeaweedResponse.reads()).flatMap { seaweedResponse =>
  //          attachmentDao.saveFile(seaweedResponse.fileId, seaweedResponse.fileName, userId, seaweedResponse.fileSize, true)
  //            .map { attachment =>
  //              // TODO old avatar file deleting
  //              userService.setAvatar(userId, attachment._id.toString)
  //              attachment
  //            }
  //
  //        }
  //      }
  //      .recover { case ex =>
  //        Logger("application").error(ex.getLocalizedMessage, ex)
  //        None
  //      }

  def getFile(id: String) = {
    attachmentDao.find(id)
      .collect {
        case attachment =>
          val url = seaweedfs_master_url + "/" + attachment.fid
          ws.url(url)
            .withMethod("GET")
            .stream()
            .map {
              response =>
                if (response.status < 300 && response.status >= 200)
                  Some(response.bodyAsSource)
                else
                  None
            }
      }
  }

  def getFileAvatar(avatarId: ObjectId, width: Option[Int]) = {
    attachmentDao.findOneById(avatarId).map {
      attachment =>
        val url = s"$seaweedfs_master_url/${
          attachment.fid
        }${
          width.fold("")(w => s"?width=$w")
        }"
        //        println(url)
        ws.url(url)
          .withMethod("GET")
          .stream()
          .collect {
            case response if response.status < 300 && response.status >= 200 =>
              response.bodyAsSource
          }.map(Some(_)).recover {
          case _ => None
        }
    }.fold {
      EitherT.leftT[Future, Source[ByteString, _]]("Can't find avatar in DB")
    } {
      EitherT.fromOptionF(_, "No pic in seaweed")
    }
  }

  def findByUserId(userId: Int) = {
    attachmentDao.findByUserId(userId)
  }

  //  def remove(userId: Int, uuid: String, path: String) = attachmentDao.remove(userId, uuid, path)
}
