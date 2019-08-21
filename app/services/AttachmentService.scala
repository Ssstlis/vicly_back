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
import models.{Attachment, SeaweedResponse, User}
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
import utils.JavaCVUtils

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

  val isBadCode = (statusCode: Int) => 200 > statusCode || statusCode >= 300

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
  val isMediaType = (mime: String) => mime match {
    case rImage(_) => true
    case rVideo(_) => true
    case _ => false
  }

  def uploadNewFile(file: File, originalFilename: String, fileSize: Long)(implicit user: User) = {

    val metadata = parseMetadata(file)

    metadata._2 match {
      case rImage(_) => saveNewFileImage(file, originalFilename, fileSize, metadata)
      case rVideo(_) => saveNewFileVideo(file, originalFilename, fileSize, metadata)
      case _ => saveNewFileOther(file, originalFilename, fileSize, metadata)
    }
  }

  def saveNewFileImage(file: File, originalFilename: String, fileSize: Long, metadata: (Map[String, String], String))(implicit user: User) = {
    val filePart = MultipartFormData.FilePart("file", originalFilename, None, FileIO.fromPath(file.toPath))
    val dataPart = DataPart("key", "value")

    //TODO Check image size and perhaps don't create previews
    val image = Image.fromFile(file)
    val small = image.scaleToWidth(480, ScaleMethod.FastScale).stream(writer)
    val big = image.scaleToWidth(1280, ScaleMethod.FastScale).stream(writer)
    val ratio = image.width / image.height.toDouble

    val bigFilePart = MultipartFormData.FilePart("file", originalFilename, None, StreamConverters.fromInputStream(() => big))
    val smallFilePart = MultipartFormData.FilePart("file", originalFilename, None, StreamConverters.fromInputStream(() => small))

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
          Left("Error")
        } else {
          val attachments = result
            .flatMap(response => response.json.asOpt(SeaweedResponse.reads()))
            .zipWithIndex
            .map { case (sw, i) => {
              val (width, height) = i match {
                case 0 => (image.width, image.height)
                case 1 => (1280, (ratio * 1280).toInt)
                case 2 => (480, (ratio * 480).toInt)
              }
              new Attachment(new ObjectId(), sw.fileId, user.id, sw.fileName, sw.fileSize, false, metadata._2, width = Some(width), height = Some(height))
            }
            }
            .toList
          attachments.get(1).flatMap(bigPreviewAttach =>
            attachments.get(2).flatMap(smallPreviewAttach =>
              attachmentDao.saveAttachment(attachments.head.copy(metadata = metadata._1, previewSmall = Some(smallPreviewAttach), previewBig = Some(bigPreviewAttach)))
            ))
            .toRight("Some error!")
        }
      )
  }

  def saveNewFileVideo(file: File, originalFilename: String, fileSize: Long, metadata: (Map[String, String], String))(implicit user: User) = {
    val filePart = MultipartFormData.FilePart("file", originalFilename, None, FileIO.fromPath(file.toPath))
    val dataPart = DataPart("key", "value")

    val (image, video, (originalWidth, originalHeight), (previewWidth, previewHeight)) = JavaCVUtils.createVideoPreview(file)

    val previewImageName = originalFilename + "_preview.jpg"
    val previewVideoName = originalFilename + "_preview.wepm"
    val cntTypeVideo = "video/webm"
    val cntTypeImage = "image/jpg"

    val imagePreviewFilePart = MultipartFormData.FilePart("file", previewImageName, None, StreamConverters.fromInputStream(() => image))
    val videoPreviewFilePart = MultipartFormData.FilePart("file", previewVideoName, None, StreamConverters.fromInputStream(() => video))

    Future.sequence(
      Seq(
        postSeaweed(Source(filePart :: dataPart :: Nil)),
        postSeaweed(Source(imagePreviewFilePart :: dataPart :: Nil)),
        postSeaweed(Source(videoPreviewFilePart :: dataPart :: Nil))
      )
    )
      .map(result =>
        if (result.exists(response => isBadCode(response.status))) {
          result.filter(response => !isBadCode(response.status)).map(successResponse =>
            successResponse.json.asOpt(SeaweedResponse.reads()).flatMap { seaweedResponse =>
              Some(deleteSeaweed(seaweedResponse.fileId))
            })
          Left("Error")
        } else {

          val swResponses = result
            .flatMap(response => response.json.asOpt(SeaweedResponse.reads()))
            .zipWithIndex
            .map { case (sw: SeaweedResponse, i: Int) => {
              val (contentType, width, height) = i match {
                case 0 => (metadata._2, originalWidth, originalHeight)
                case 1 => (cntTypeImage, previewWidth, previewHeight)
                case 2 => (cntTypeVideo, previewWidth, previewHeight)
              }
              new Attachment(new ObjectId(), sw.fileId, user.id, sw.fileName, sw.fileSize, false, contentType, width = Some(width), height = Some(height))
            }
            }
            .toList
          swResponses.get(1).flatMap(imagePreview =>
            swResponses.get(2).flatMap(videoPreview =>
              attachmentDao.saveAttachment(swResponses.head.copy(metadata = metadata._1, previewSmall = Some(imagePreview), previewBig = Some(videoPreview)))))
            .toRight("Some error!")
        }
      )
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
        }.toRight {
          "Error while save in MongoDB"
        }
      }
      .recover { case ex =>
        Logger("application").error(ex.getLocalizedMessage, ex)
        Left(ex.getLocalizedMessage)
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

  def getFile(id: String) = {
    attachmentDao.find(id)
      .collect {
        case attachment =>
          var url: String = seaweedfs_master_url + "/" + attachment.fid
          //          if (isImageType(attachment.mime) && width.isDefined) {
          //            url += "?width=" + width.get
          //          }
          ws.url(url)
            .withMethod("GET")
            .stream()
            .map {
              response =>
                if (response.status < 300 && response.status >= 200) {
                  Some((response.bodyAsSource, if (response.header("Content-Length").isDefined) Some(response.header("Content-Length").get.toLong) else None, attachment.mime))
                }
                else {
                  None
                }
            }
      }
  }

  def getFilePreviewSmall(id: String) = {
    attachmentDao.find(id)
      .flatMap { attachment =>
        attachment.previewSmall.map {
          case attachment if isMediaType(attachment.mime) =>
            var url: String = seaweedfs_master_url + "/" + attachment.fid
            ws.url(url)
              .withMethod("GET")
              .stream()
              .map {
                response =>
                  if (response.status < 300 && response.status >= 200) {
                    Right((response.bodyAsSource, if (response.header("Content-Length").isDefined) Some(response.header("Content-Length").get.toLong) else None, attachment.mime))
                  }
                  else {
                    Left("Attachment with provided id not exist in SeaweedFS")
                  }
              }
          case _ => Future.successful(Left("Attachment with provided id not exist"))
        }
      }.getOrElse(Future.successful(Left("Attachment not exist or this attachment isn't meadia file!")))
  }

  def getFilePreviewBig(id: String) = {
    attachmentDao.find(id)
      .flatMap { attachment =>
        attachment.previewBig.map {
          case attachment if isMediaType(attachment.mime) =>
            var url: String = seaweedfs_master_url + "/" + attachment.fid
            ws.url(url)
              .withMethod("GET")
              .stream()
              .map {
                response =>
                  if (response.status < 300 && response.status >= 200) {
                    Right((response.bodyAsSource, if (response.header("Content-Length").isDefined) Some(response.header("Content-Length").get.toLong) else None, attachment.mime))
                  }
                  else {
                    Left("Attachment with provided id not exist in SeaweedFS")
                  }
              }
          case _ => Future.successful(Left("Attachment with provided id not exist"))
        }
      }.getOrElse(Future.successful(Left("Attachment not exist or this attachment isn't meadia file!")))
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
