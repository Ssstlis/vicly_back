package controllers

import actions.AuthUtils
import akka.stream.scaladsl.Source
import cats.data._
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.mvc.InjectedController
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
    *                       "Content-Type":"image/png",
    *                       "Chroma ColorSpaceType":"RGB",
    *                       "Compression NumProgressiveScans":"1",
    *                       "Transparency Alpha":"nonpremultipled"
    *                    },
    *                    "previewSmall": {
    *                       "id": "5d41ffa84af6f37daebf8154",
    *                       "fid": "1,5fc6bb9cec",
    *                       "user_id": 1,
    *                       "filename": "8K_Ultra_186.jpg",
    *                       "size": 35182,
    *                       "is_avatar": false,
    *                       "mime": "image/jpeg",
    *                       "metadata": {
    *                         "fulfilled": "false"
    *                       },
    *                       "timestamp": 1564606376
    *                     },
    *                     "previewBig": {
    *                       "id": "5d41ffa84af6f37daebf8153",
    *                       "fid": "1,5e26c5a421",
    *                       "user_id": 1,
    *                       "filename": "8K_Ultra_186.jpg",
    *                       "size": 224063,
    *                       "is_avatar": false,
    *                       "mime": "image/jpeg",
    *                       "metadata": {
    *                         "fulfilled": "false"
    *                       },
    *                       "timestamp": 1564606376
    *     }
    *                    }
    * @apiDescription Upload new file as multipart form-data. Return JSON about uploaded attachment.
    */
  def upload = authUtils.authenticateAction.async(parse.multipartFormData) { request =>
    implicit val user = request.user

    request.body.file("file").map { file =>
      attachmentService.uploadNewFile(file.ref.toFile, file.filename, file.fileSize)
        .map {
          case Right(attachment) => Ok(Json.toJson(attachment))
          case Left(error) => BadRequest(Json.obj("error" -> error))
        }
    }.getOrElse(Future {
      NotFound
    })
  }

  /**
    * @api {POST} /api/attachment/upload_avatar  Upload new avatar
    * @apiName Upload new avatar
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
  def uploadAvatar = authUtils.authenticateAction.async(parse.multipartFormData) {
    request =>
      val user = request.user

      //TODO Remove old avatar !!!
      request.body.file("file").map {
        file =>
          attachmentService.saveFileAvatarNew(user, file.ref.toFile, file.filename, user.id)
            .map {
              case Left(ex) => BadRequest(Json.obj("error" -> ex.getLocalizedMessage))
              case Right(attachment) => Ok(Json.toJson(attachment))
            }
      }.getOrElse(Future.successful(NotFound(Json.obj("error" -> "There is no file in formdata!"))))
  }

  /**
    * @api {GET} /api/attachment/download/:id  Download file
    * @apiName Download file
    * @apiGroup Attachment
    * @apiParam {Int}             id               Id of attachment.
    * @apiDescription Download file. Use other routes for download media previews
    */
  def download(id: String) = authUtils.authenticateAction.async {
    request =>
      attachmentService.getFile(id).map {
        fileFuture =>
          fileFuture.map {
            optStream =>
              optStream.map {
                case (source, size: Option[Long], mime) => Ok.streamed(source, size, Some(mime))
              }.getOrElse(Gone)
          }
      }.getOrElse(Future {
        NotFound
      })
  }

  /**
    * @api {GET} /api/attachment/download/:id/preview_small  Download small preview for media-file
    * @apiName Download small preview
    * @apiGroup Attachment
    * @apiParam {Int}             id               Id of attachment.
    * @apiDescription Download small preview for media-file.For image is pic with 200px width and same proportion. For video preview is pic of of video start.(Eng WTF)
    */
  def downloadPreviewSmall(id: String) = authUtils.authenticateAction.async {
    request =>
      attachmentService.getFilePreviewSmall(id).map {
        case Right((source, size: Option[Long], mime)) => Ok.streamed(source, size, Some(mime))
        case Left(err) => Gone(Json.obj("error" -> err))
      }
  }

  /**
    * @api {GET} /api/attachment/download/:id/preview_big  Download big preview for media-file
    * @apiName Download big preview
    * @apiGroup Attachment
    * @apiParam {Int}             id               Id of attachment.
    * @apiDescription Download big preview for media-file. For image is HD pic. For video big preview is webm video.
    */
  def downloadPreviewBig(id: String) = authUtils.authenticateAction.async {
    request =>
      attachmentService.getFilePreviewBig(id).map {
        case Right((source, size: Option[Long], mime)) => Ok.streamed(source, size, Some(mime))
        case Left(err) => Gone(Json.obj("error" -> err))
      }
  }

  /**
    * @api {POST} /api/attachment/download_avatar/:user_id  Download user avatar
    * @apiName Download user avatar
    * @apiGroup Attachment
    * @apiParam {Int}             user_id               Id of user.
    * @apiParam {Int}             [width=None]          Optional width of attachment if file is image.
    * @apiDescription Download user avatar.
    */
  def downloadAvatar(userId: Int, width: Option[Int]) = authUtils.authenticateAction.async {
    request =>
      (for {
        user <- EitherT.fromOption[Future](userService.findOne(userId), "Can't find user")
        avatar <- EitherT.fromOption[Future](user.avatar, "No avatar specified for user")
        stream <- attachmentService.getFileAvatar(avatar, width)
      } yield {
        Ok.sendEntity(HttpEntity.Streamed(stream, None, None))
      }).valueOr(err => BadRequest(Json.obj("error" -> err)))
  }

  /**
    * @api {GET} /api/attachment/:id   Get attach info
    * @apiName Get attach info
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
  def getAttachment(id: String) = authUtils.authenticateAction {
    _ =>
      Ok(Json.toJson(attachmentService.findById(id)))
  }

  /**
    * @api {GET} /api/attachment/list   Get all attachment info
    * @apiName Get all attachment info
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
  def list = authUtils.authenticateAction {
    request =>
      val user = request.user
      Ok(Json.toJson(attachmentService.findByUserId(user.id)))
  }

  def remove(id: String) = authUtils.authenticateAction {
    _ =>
      attachmentService.remove(id) match {
        case true => Ok
        case _ => BadRequest
      }
  }
}
