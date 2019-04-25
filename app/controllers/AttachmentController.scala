package controllers

import java.io.{File, FileInputStream}

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.mvc.InjectedController
import services.{AttachmentService, UserService}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import cats.implicits._
import cats.data._
import org.apache.tika.metadata._
import org.bson.types.ObjectId
import org.xml.sax.SAXException

@Singleton
class AttachmentController @Inject()(
  attachmentService: AttachmentService,
  authUtils: AuthUtils,
  config: Configuration,
  userService: UserService
)(implicit ec: ExecutionContext) extends InjectedController {

  val path = config.get[String]("path.upload")

  //  def upload(isAvatar: Int) = authUtils.authenticateAction(parse.multipartFormData) { request =>
  //    val user = request.user
  //    (for {
  //      path <- config.getOptional[String]("path.upload")
  //      groupId = user.groupId
  //      file <- request.body.file("file")
  //      if file.ref.toFile.length() <= 3 * (1 << 20)
  //      uuid = randomUUID().toString
  //      dirPath = s"$path/$groupId/$uuid"
  //      tempPath = s"${file.ref.path.toString}"
  //      if attachmentService.saveFile(tempPath, dirPath, file.filename.replaceAll("\\s", "_"), user.id, file.ref.length())
  //    } yield {
  //      if (isAvatar == 1 && file.contentType.exists(_.contains("image"))) userService.setAvatar(user.id, uuid)(groupId)
  //      Ok(uuid)
  //    }).getOrElse(BadRequest)
  //  }

  import org.apache.tika.exception.TikaException
  import org.apache.tika.parser.AutoDetectParser
  import org.apache.tika.sax.BodyContentHandler
  import java.io.IOException
  import java.io.InputStream

  @throws[IOException]
  @throws[SAXException]
  @throws[TikaException]
  def parseExample(file: File): Map[String, String] = {
    val parser = new AutoDetectParser
    val handler = new BodyContentHandler
    val metadata = new Metadata
    try {
      val stream = new FileInputStream(file)
      try {
        parser.parse(stream, handler, metadata)
        (for (key <- metadata.names()) yield (key, metadata.get(key))).toMap
      } finally if (stream != null) stream.close()
    } catch {
      case err: Throwable => {
        println(err.toString)
        Map("fulfilled" -> "error")
      }
    }
  }

  def upload = authUtils.authenticateAction.async(parse.multipartFormData) { request =>
    val user = request.user

    request.body.file("file").map { file =>
      val metadata = parseExample(file.ref.toFile)
      attachmentService.saveFileNew(file.ref.toFile, file.filename, user.id, isAvatar = false, metadata)
        .map { response =>
          Ok(Json.toJson(response))
        }
    }.getOrElse(Future {
      NotFound
    })
  }

  def uploadAvatar = authUtils.authenticateAction.async(parse.multipartFormData) { request =>
    val user = request.user

    //TODO Remove old avatar !!!
    request.body.file("file").map { file =>
      attachmentService.saveFileAvatarNew(user, file.ref.toFile, file.filename, user.id)
        .map {
          case Left(ex) => BadRequest(Json.obj("error" -> ex.getLocalizedMessage))
          case Right(attachment) => Ok(Json.toJson(attachment))
        }
    }.getOrElse(Future.successful(NotFound(Json.obj("error" -> "There is no file in in formdata!"))))
  }

  def download(id: String) = authUtils.authenticateAction.async { request =>
    val user = request.user

    attachmentService.getFile(id).collect { case file =>
      file.map { optStream =>
        optStream.map { stream =>
          Ok.sendEntity(HttpEntity.Streamed(stream, None, None))
        }.getOrElse(Gone)
      }
    }.getOrElse(Future {
      NotFound
    })
  }

  def downloadAvatar(userId: Int, width: Option[Int]) = authUtils.authenticateAction.async { request =>
    (for {
      user <- EitherT.fromOption[Future](userService.findOne(userId), "Can't find user")
      avatar <- EitherT.fromOption[Future](user.avatar, "No avatar specified for user")
      stream <- attachmentService.getFileAvatar(avatar, width)
    } yield {
      Ok.sendEntity(HttpEntity.Streamed(stream, None, None))
    }).valueOr(err => BadRequest(Json.obj("error" -> err)))
  }

  def list = authUtils.authenticateAction { request =>
    val user = request.user
    Ok(Json.toJson(attachmentService.findByUserId(user.id)))
  }

  // TODO
  //  def listById = authUtils.authenticateAction { request =>
  //    val user = request.user
  //    val json = request.body
  //
  //    for (
  //    attachments <- (json \ "attachments").asOpt[List[ObjectId]]
  //      atta
  //    <- attachmentService
  //    .
  //    )
  //    Ok(Json.toJson(attachmentService.findByUserId(user.id)))
  //  }

  //    def remove(id: String) = authUtils.authenticateAction { request =>
  //      val user = request.user
  //      (for {
  //        groupId <- user.groupId
  //        _ <- attachmentService.remove(user.id, uuid, s"$path/$groupId/$uuid")
  //      } yield {
  //        Ok
  //      }).getOrElse(BadRequest)
  //    }
}
