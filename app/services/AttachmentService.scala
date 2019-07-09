package services

import java.io.{File, FileInputStream, IOException}

import akka.stream.scaladsl.{FileIO, Source, StreamConverters}
import akka.util.ByteString
import cats.data._
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.{Image, ScaleMethod}
import daos.AttachmentDao
import models.{SeaweedResponse, User}
import org.apache.tika.Tika
import org.apache.tika.exception.TikaException
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler
import org.bson.types.ObjectId
import org.xml.sax.SAXException
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData._
import play.api.{Configuration, Logger}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

@Singleton
class AttachmentService @Inject()(
  attachmentDao: AttachmentDao,
  config: Configuration,
  userService: UserService,
  ws: WSClient
)(implicit ec: ExecutionContext) {

  val isImageType: String => Boolean = (contentType: String) => contentType.startsWith("image/")

  val isBadCode: Int => Boolean = (statusCode: Int) => 200 < statusCode || statusCode >= 300

  val seaweedfs_volume_url: String = config.get[String]("seaweed.address.volume")
  val seaweedfs_master_url: String = config.get[String]("seaweed.address.master")

  def postSeaweed(source: Source[MultipartFormData.Part[Source[ByteString, _]], _]) = {
    ws.url(seaweedfs_volume_url + "/submit")
      .withRequestTimeout(30.seconds)
      .post(source)
  }

  def deleteSeaweed(fid: String) = {
    ws.url(s"$seaweedfs_master_url/${fid}")
      .delete()
  }

  //TODO Replace code!
  @throws[IOException]
  @throws[SAXException]
  @throws[TikaException]
  def parseMetadata(file: File): (Map[String, String], String) = {
    val parser = new AutoDetectParser
    val handler = new BodyContentHandler
    val metadata = new Metadata

    val tika = new Tika
    try {
      val stream = new FileInputStream(file)
      try {
        parser.parse(stream, handler, metadata)
        val mimeType = tika.detect(file)
        ((for (key <- metadata.names()) yield (key, metadata.get(key))).toMap, mimeType)
      } finally if (stream != null) stream.close()
    } catch {
      case err: Throwable => {
        println(err.toString)
        (Map("fulfilled" -> "error"), "application/octet-stream")
      }
    }
  }

  implicit val writer = JpegWriter().withCompression(50).withProgressive(true)
  val rImage = "^image/(.*)".r
  val rVideo = "^video/(.*)".r

  def uploadNewFile(file: File, originalFilename: String, fileSize: Long)(implicit user: User) = {

    val metadata = parseMetadata(file)

    metadata._2 match {
      case rImage(_) => saveNewFileImage(file, originalFilename, fileSize, metadata)
      case _ => saveNewFileOther(file, originalFilename, fileSize, metadata)
    }
  }

  def saveNewFileImage(file: File, originalFilename: String, fileSize: Long, metadata: (Map[String, String], String))(implicit user: User) = {
    val filePart = MultipartFormData.FilePart("file", originalFilename, None, FileIO.fromPath(file.toPath))
    val dataPart = DataPart("key", "value")

    val image = Image.fromFile(file)
    val small = image.scaleToWidth(480, ScaleMethod.FastScale).stream(writer)
    val big = image.scaleToWidth(1280, ScaleMethod.FastScale).stream(writer)

    val bigFilePart = MultipartFormData.FilePart("file", originalFilename, None, StreamConverters.fromInputStream(() => small))
    val smallFilePart = MultipartFormData.FilePart("file", originalFilename, None, StreamConverters.fromInputStream(() => big))

    Future.sequence(
      Seq(
        postSeaweed(Source(filePart :: dataPart :: Nil)),
        postSeaweed(Source(bigFilePart :: dataPart :: Nil)),
        postSeaweed(Source(smallFilePart :: dataPart :: Nil))
      )
    )
      .map(result =>
        if (result.exists(response => isBadCode(response.status))) {
          result.filter(response => !isBadCode(response.status)).map(successResponse =>
            successResponse.json.asOpt(SeaweedResponse.reads()).flatMap { seaweedResponse =>
              Some(deleteSeaweed(seaweedResponse.fileId))
            })
          None
        } else {
          val swResponses = result.flatMap(response =>
            response.json.asOpt(SeaweedResponse.reads())
          )
          val previewAttachs = swResponses.tail.flatMap(sw =>
            attachmentDao.saveFile(sw.fileId, sw.fileName, user.id, sw.fileSize, false, Map.empty, metadata._2)
          ).toList
          attachmentDao.saveFile(swResponses.head.fileId, swResponses.head.fileName, user.id, swResponses.head.fileSize, false, metadata._1, metadata._2)
            .flatMap(attachment =>
              if (attachmentDao.updateMetaAndPreview(attachment, metadata._1, previewAttachs.get(0).get._id, previewAttachs.get(0).get._id)) {
                attachmentDao.find(attachment._id)
              } else None
            )

        }
      )
//    ws.url(seaweedfs_volume_url + "/submit")
//      .withRequestTimeout(30.seconds)
//      .post(Source(filePart :: dataPart :: Nil))
//      .map { response =>
//        println(response.body)
//        response.json.asOpt(SeaweedResponse.reads()).flatMap { seaweedResponse =>
//          attachmentDao.saveFile(seaweedResponse.fileId, seaweedResponse.fileName, user.id, seaweedResponse.fileSize, false, metadata._1, metadata._2)
//        }
//      }
//      .recover { case ex =>
//        Logger("application").error(ex.getLocalizedMessage, ex)
//        None
//      }
//
//    ws.url(seaweedfs_volume_url + "/submit")
//      .withRequestTimeout(30.seconds)
//      .post(Source(filePart :: dataPart :: Nil))
//      .map { response =>
//        println(response.body)
//        response.json.asOpt(SeaweedResponse.reads()).flatMap { seaweedResponse =>
//          attachmentDao.saveFile(seaweedResponse.fileId, seaweedResponse.fileName, user.id, seaweedResponse.fileSize, false, metadata._1, metadata._2)
//        }
//      }
//      .recover { case ex =>
//        Logger("application").error(ex.getLocalizedMessage, ex)
//        None
//      }
  }

  def saveNewFileOther(file: File, originalFilename: String, fileSize: Long, metadata: (Map[String, String], String))(implicit user: User) = {
    val filePart = MultipartFormData.FilePart("file", originalFilename, None, FileIO.fromPath(file.toPath))
    val dataPart = DataPart("key", "value")

    ws.url(seaweedfs_volume_url + "/submit")
      .withRequestTimeout(30.seconds)
      .post(Source(filePart :: dataPart :: Nil))
      .map { response =>
        println(response.body)
        response.json.asOpt(SeaweedResponse.reads()).flatMap { seaweedResponse =>
          attachmentDao.saveFile(seaweedResponse.fileId, seaweedResponse.fileName, user.id, seaweedResponse.fileSize, false, metadata._1, metadata._2)
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
              attachment <- attachmentDao.saveFile(seaweedResponse.fileId, seaweedResponse.fileName, userId, seaweedResponse.fileSize, isAvatar = true, Map.empty, "image/*")
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

  def getFile(id: String, width: Option[Int]) = {
    attachmentDao.find(id)
      .collect {
        case attachment =>
          var url: String = seaweedfs_master_url + "/" + attachment.fid
          if (isImageType(attachment.mime) && width.isDefined) {
            url += "?width=" + width.get
          }

          ws.url(url)
            .withMethod("GET")
            .stream()
            .map {
              response =>
                if (response.status < 300 && response.status >= 200) {
                  val lenght = if (response.header("Content-Length").isDefined) response.header("Content-Length").get.toLong else null
                  Some((response.bodyAsSource, Option(lenght), attachment.mime))
                }
                else {
                  None
                }

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
          }.map(Some(_))
          .recover {
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

  def findById(id: String) = {
    attachmentDao.findById(id)
  }

  def remove(uuid: String) = attachmentDao.remove(uuid)
}
