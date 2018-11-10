package controllers

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import models.Message
import pdi.jwt.JwtAlgorithm
import play.api.libs.json.Json
import play.api.mvc.InjectedController
import services.{ChatService, MessageService}
import utils.CollectionHelper.TraversableOnceHelper

@Singleton
class MainController @Inject()(
  authUtils: AuthUtils,
  chatService: ChatService,
  messageService: MessageService
) extends InjectedController {

  val algo = Seq(JwtAlgorithm.HS256, JwtAlgorithm.HMD5, JwtAlgorithm.HS384)

  val key = "secretKey"

  def messages = Action {
    val messages = messageService.all
    val chats = messages.map(_.chatId).distinct
    val chatMap = chatService.findByIds(chats).zipBy(_.id)
    val json = messages.map { message =>
      Json.toJson(message)(Message.writes(chatMap.get(message.chatId)))
    }
    Ok(Json.toJson(json))
  }

  def postMessage = authUtils.authenticateAction(parse.json) { request =>
    val user = request.user
    request.body.validate(Message.reads(user._id)).map { message =>
      val isWrite = messageService.add(message).wasAcknowledged()
      if (isWrite) Ok else ResetContent
    }.getOrElse(BadRequest)
  }
}