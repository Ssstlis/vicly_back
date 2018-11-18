package controllers

import java.util.UUID.randomUUID

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import models.{Invite, User}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.mvc.InjectedController
import services.{InviteService, UserService}
import utils.Helper.DateTimeExtended

@Singleton
class InviteController @Inject()(
  authUtils: AuthUtils,
  inviteService: InviteService,
  userService: UserService
) extends InjectedController {

  def create = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      groupId <- user.groupId
      firstName <- (json \ "first_name").asOpt[String]
      lastName <- (json \ "last_name").asOpt[String]
    } yield {
      val uuid = randomUUID().toString
      val invite = Invite(firstName, lastName, uuid, groupId, user.id)
      if (inviteService.create(invite).wasAcknowledged()) {
        Ok(Json.obj("invite_id" -> uuid))
      } else {
        NotImplemented
      }
    }).getOrElse(BadRequest)
  }

  def one(uuid: String) = Action {
    inviteService.find(uuid).map(invite => Ok(Json.toJson(invite))).getOrElse(BadRequest)
  }

  def list = Action {
    Ok(Json.toJson(inviteService.all))
  }

  def signup = Action(parse.json) { request =>
    val json = request.body
    (for {
      uuid <- (json \ "uuid").asOpt[String]
      invite <- inviteService.find(uuid)
      login <- (json \ "login").asOpt[String]
      password <- (json \ "password").asOpt[String]
    } yield {
      val id = userService.nextId
      val user = User(
        new ObjectId(),
        invite.firstName,
        invite.lastName,
        Some(invite.groupId),
        password,
        login,
        active = false,
        DateTime.now.timestamp,
        id)
      if (userService.save(user).wasAcknowledged() && inviteService.remove(invite).wasAcknowledged()) {
        Ok
      } else {
        Conflict
      }
    }).getOrElse(BadRequest)
  }
}
