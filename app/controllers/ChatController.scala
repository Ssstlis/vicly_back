package controllers

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import play.api.mvc.InjectedController
import services.{ChatService, UserService}

@Singleton
class ChatController @Inject()(
  authUtils: AuthUtils,
  chatService: ChatService,
  userService: UserService
) extends InjectedController {

  def createGroupChat = authUtils.authenticateAction(parse.json) { request =>
    val user = request.user
    val json = request.body

    (for {
      groupId <- user.groupId
      userIds <- (json \ "user_ids").asOpt[List[Int]] if userIds.nonEmpty
      if userService.usersSameGroupNonArchive(userIds, groupId)
      ids = {
        println(1)
        userIds.distinct
      }
      if chatService.createGroupChat(ids, groupId, user._id)
    } yield {
      Ok
    }).getOrElse(BadRequest)
  }

  def addUserToChat = authUtils.authenticateAction(parse.json) { _ =>
    Ok
  }
}
