package controllers

import actions.AuthUtils
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

  def upload = authUtils.authenticateAction.async(parse.multipartFormData) { request =>
    val user = request.user

    request.body.file("file").map { file =>
      attachmentService.saveFileNew(file.ref.toFile, file.filename, user.id, isAvatar = false)
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
      attachmentService.saveFileNew(file.ref.toFile, file.filename, user.id, isAvatar = true)
        .map { response =>
          Ok(Json.toJson(response))
        }
    }.getOrElse(Future {
      NotFound
    })
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
    userService.findOne(userId).map { user =>
      user.avatar.map { avatar =>
        attachmentService.getFileAvatar(avatar, width).collect { case file =>
          file.map { optStream =>
            optStream.map { stream =>
              Ok.sendEntity(HttpEntity.Streamed(stream, None, None))
            }.getOrElse(Gone)
          }
        }.getOrElse(Future.successful(NotFound))
      }.getOrElse(Future.successful(NotFound))
    }.getOrElse(Future {
      NotFound
    })
  }

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
