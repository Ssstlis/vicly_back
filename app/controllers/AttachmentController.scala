package controllers

import java.util.UUID.randomUUID

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import models.SeaweedResponse
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.InjectedController
import services.{AttachmentService, UserService}

import scala.util.{Failure, Success}

@Singleton
class AttachmentController @Inject()(
                                      attachmentService: AttachmentService,
                                      authUtils: AuthUtils,
                                      config: Configuration,
                                      userService: UserService
                                    )(implicit ec: ExecutionContext) extends InjectedController {

  val path = config.get[String]("path.upload")

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

  def uploadnew = Action.async(parse.multipartFormData) { request =>
    request.body.file("file").map { file =>
      attachmentService.saveFileNew(5, file.ref.toFile).map { status =>
        Ok(Json.toJson("status"-> status))
      }
    }.getOrElse(Future.successful(BadRequest))
    //    val user = request.user
    //    request.body.file("file").map { file =>
    //      attachmentService.saveFileNew(5, file.ref.toFile)
    //        .map { response =>
    ////          response.json.asOpt(SeaweedResponse.reads()).map { seaweedresp =>
    //            Ok
    //          }.getOrElse(Forbidden)
    //        }
    //    }.getOrElse(Future {
    //      NotFound
    //    })
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

  def remove(uuid: String) = authUtils.authenticateAction { request =>
    val user = request.user
    (for {
      groupId <- user.groupId
      _ <- attachmentService.remove(user.id, uuid, s"$path/$groupId/$uuid")
    } yield {
      Ok
    }).getOrElse(BadRequest)
  }
}
