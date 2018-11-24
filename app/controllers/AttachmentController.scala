package controllers

import java.util.UUID.randomUUID

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.InjectedController
import services.{AttachmentService, UserService}

@Singleton
class AttachmentController @Inject()(
  attachmentService: AttachmentService,
  authUtils: AuthUtils,
  config: Configuration,
  userService: UserService
)(implicit ec: ExecutionContext) extends InjectedController {

  def upload(isAvatar: Int) = authUtils.authenticateAction(parse.multipartFormData) { request =>
    val user = request.user
    (for {
      path <- config.getOptional[String]("path.upload")
      groupId <- user.groupId
      file <- request.body.file("file")
      if file.ref.toFile.length() <= 3 * (1 << 20)
      uuid = randomUUID().toString
      dirPath = s"$path/$groupId/$uuid"
      tempPath = s"${file.ref.path.toString}"
      if attachmentService.saveFile(tempPath, dirPath, file.filename.replaceAll("\\s", "_"), user.id, file.ref.length())
    } yield {
      if (isAvatar == 1 && file.contentType.exists(_.contains("image"))) userService.setAvatar(user.id, uuid)(groupId)
      Ok(uuid)
    }).getOrElse(BadRequest)
  }

  def download(uuid: String) = authUtils.authenticateAction { request =>
    val user = request.user
    attachmentService.find(uuid, user.groupId).map(file =>
      Ok.sendFile(file)
    ).getOrElse(BadRequest)
  }

  def list = authUtils.authenticateAction { request =>
    val user = request.user
    Ok(Json.toJson(attachmentService.findByUserId(user.id)))
  }
}
