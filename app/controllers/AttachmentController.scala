package controllers

import java.io.{File, FileInputStream}

import actions.AuthUtils
import akka.protobuf.ByteString
import akka.stream.scaladsl.Source
import cats.data._
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import org.apache.tika.Tika
import org.apache.tika.metadata._
import org.xml.sax.SAXException
import play.api.Configuration
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.mvc.{InjectedController, ResponseHeader, Result}
import services.{AttachmentService, UserService}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

@Singleton
class AttachmentController @Inject()(
                                      attachmentService: AttachmentService,
                                      authUtils: AuthUtils,
                                      config: Configuration,
                                      userService: UserService
                                    )(implicit ec: ExecutionContext) extends InjectedController {

  val path = config.get[String]("path.upload")


  import java.io.IOException

  import org.apache.tika.exception.TikaException
  import org.apache.tika.parser.AutoDetectParser
  import org.apache.tika.sax.BodyContentHandler

  //TODO Replace code!
  @throws[IOException]
  @throws[SAXException]
  @throws[TikaException]
  def parseExample(file: File): Tuple2[Map[String, String], String] = {
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


  /**
    * @api {POST} /api/attachment/upload  Upload file
    * @apiName Upload file
    * @apiGroup Attachment
    * @apiSuccessExample {json} Success-Response:
    *                    HTTP/1.1 200 OK
    *                    {
    *                    "id":"5d2380dfa7b11b000118eff0",
    *                    "user_id":1,
    *                    "filename":"713040.png",
    *                    "size":2289429,
    *                    "timestamp":1562607839,
    *                    "is_avatar":false,
    *                    "mime":"image/png",
    *                    "metadata":{
    *                    "Content-Type":"image/png",
    *                    "Chroma ColorSpaceType":"RGB",
    *                    "Compression NumProgressiveScans":"1",
    *                    "Transparency Alpha":"nonpremultipled"
    *                    }
    *                    }
    * @apiDescription Upload new file as multipart form-data. Return JSON about uploaded attachment.
    */
  def upload = authUtils.authenticateAction.async(parse.multipartFormData) { request =>
    val user = request.user

    request.body.file("file").map { file =>
      val (metadata, mime) = parseExample(file.ref.toFile)
      attachmentService.saveFileNew(file.ref.toFile, file.filename, user.id, isAvatar = false, metadata, mime)
        .map { response =>
          Ok(Json.toJson(response))
        }
    }.getOrElse(Future {
      NotFound
    })
  }

  /**
    * @api {POST} /api/attachment/upload_avatar  Upload new avatar
    * @apiName  Upload new avatar
    * @apiGroup Attachment
    * @apiSuccessExample {json} Success-Response:
    *                    HTTP/1.1 200 OK
    *                    {
    *                    "id":"5d2380dfa7b11b000118eff0",
    *                    "user_id":1,
    *                    "filename":"713040.png",
    *                    "size":2289429,
    *                    "timestamp":1562607839,
    *                    "is_avatar":false,
    *                    "mime":"image/png",
    *                    "metadata":{}
    *                    }
    * @apiDescription Upload new avatar. Return JSON about uploaded attachment.
    */
  def uploadAvatar = authUtils.authenticateAction.async(parse.multipartFormData) { request =>
    val user = request.user

    //TODO Remove old avatar !!!
    request.body.file("file").map { file =>
      attachmentService.saveFileAvatarNew(user, file.ref.toFile, file.filename, user.id)
        .map {
          case Left(ex) => BadRequest(Json.obj("error" -> ex.getLocalizedMessage))
          case Right(attachment) => Ok(Json.toJson(attachment))
        }
    }.getOrElse(Future.successful(NotFound(Json.obj("error" -> "There is no file in formdata!"))))
  }


  /**
    * @api {POST} /api/attachment/download/:id  Download file
    * @apiName  Download file
    * @apiGroup Attachment
    * @apiParam {Int}             id               Id of attachment.
    * @apiParam {Int}             [width=None]     Optional width of attachment if file is image.
    * @apiDescription Download file.
    */
  def download(id: String, width: Option[Int]) = authUtils.authenticateAction.async { request =>

    attachmentService.getFile(id, width).map { fileFuture =>
      fileFuture.map { optStream =>
        optStream.map { case (source, size:Option[Long], mime) =>
          size match {
            case Some(size) => Ok.streamed(source, Some(size), Some(mime))
            case None => Ok.chunked(source).as(mime)
          }

        }.getOrElse(Gone)
      }
    }.getOrElse(Future {
      NotFound
    })
  }

  /**
    * @api {POST} /api/attachment/download_avatar/:user_id  Download user avatar
    * @apiName  Download user avatar
    * @apiGroup Attachment
    * @apiParam {Int}             user_id               Id of user.
    * @apiParam {Int}             [width=None]          Optional width of attachment if file is image.
    * @apiDescription Download user avatar.
    */
  def downloadAvatar(userId: Int, width: Option[Int]) = authUtils.authenticateAction.async { request =>
    (for {
      user <- EitherT.fromOption[Future](userService.findOne(userId), "Can't find user")
      avatar <- EitherT.fromOption[Future](user.avatar, "No avatar specified for user")
      stream <- attachmentService.getFileAvatar(avatar, width)
    } yield {
      Ok.sendEntity(HttpEntity.Streamed(stream, None, None))
    }).valueOr(err => BadRequest(Json.obj("error" -> err)))
  }

  /**
    * @api {POST} /api/attachment/:id   Get attach info
    * @apiName  Get attach info
    * @apiGroup Attachment
    * @apiParam {Int}             if               Id of attachment.
    * @apiSuccessExample {json} Success-Response:
    *                    HTTP/1.1 200 OK
    *                    {
    *                    "id":"5d2380dfa7b11b000118eff0",
    *                    "user_id":1,
    *                    "filename":"713040.png",
    *                    "size":2289429,
    *                    "timestamp":1562607839,
    *                    "is_avatar":false,
    *                    "mime":"image/png",
    *                    "metadata":{}
    *                    }
    * @apiDescription Download user avatar.
    */
  def getAttachment(id: String) = authUtils.authenticateAction { request =>
    Ok(Json.toJson(attachmentService.findById(id)))
  }

  /**
    * @api {POST} /api/attachment/list   Get all attachment info
    * @apiName  Get all attachment info
    * @apiGroup Attachment
    * @apiSuccessExample {json} Success-Response:
    *                    HTTP/1.1 200 OK
    *                    [{
    *                    "id":"5d2380dfa7b11b000118eff0",
    *                    "user_id":1,
    *                    "filename":"713040.png",
    *                    "size":2289429,
    *                    "timestamp":1562607839,
    *                    "is_avatar":false,
    *                    "mime":"image/png",
    *                    "metadata":{}
    *                    },
    *                    {
    *                    "id":"5d2380dfa7b11b000118eff1",
    *                    "user_id":1,
    *                    "filename":"71305465445645640.png",
    *                    "size":2289429,
    *                    "timestamp":1562607839,
    *                    "is_avatar":false,
    *                    "mime":"image/jpg",
    *                    "metadata":{}
    *                    }]
    * @apiDescription Download user avatar.
    */
  def list = authUtils.authenticateAction { request =>
    val user = request.user
    Ok(Json.toJson(attachmentService.findByUserId(user.id)))
  }

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
