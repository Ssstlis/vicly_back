package controllers

import java.io.File
import java.util.UUID.randomUUID

import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import scala.sys.process._
import scala.util.Try

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.InjectedController
import services.UserService

@Singleton
class AttachmentController @Inject()(
  authUtils: AuthUtils,
  config: Configuration,
  userService: UserService
)(implicit ec: ExecutionContext) extends InjectedController {

  def sh(cmd: String) = {
    Seq("sh", "-c") :+ cmd
  }

  def upload(isAvatar: Boolean) = authUtils.authenticateAction(parse.multipartFormData) { request =>
    val user = request.user
    (for {
      path <- config.getOptional[String]("path.upload")
      groupId <- user.groupId
      file <- request.body.file("file")
      if file.ref.toFile.length() <= 3 * (1 << 20)
      uuid = randomUUID().toString
      dirPath = s"$path/$groupId/$uuid"
      tempPath = s"${file.ref.path.toString}"
      if (
           sh(s"mkdir -p $dirPath") #&&
           sh(s"cp $tempPath $dirPath/${file.filename}") !
         ) == 0
    } yield {
      println(file)
      if (isAvatar) userService.setAvatar(user.id, uuid)(groupId)
      Ok(uuid)
    }).getOrElse(BadRequest)
  }

  def download(uuid: String) = authUtils.authenticateAction { request =>
    (for {
      path <- config.getOptional[String]("path.upload")
      groupId <- request.user.groupId
      filePath = s"$path/$groupId/$uuid"
      output <- Try(sh(s"ls $filePath -1") !!).toOption
      fileName <- output.split("\\n").toList.headOption
      file <- Try(new File(s"$filePath/$fileName")).toOption
    } yield {
      Ok.sendFile(file)
    }).getOrElse(BadRequest)
  }
}
