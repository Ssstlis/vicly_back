package controllers

import java.util.UUID.randomUUID

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import models.{Invite, User}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, InjectedController}
import services._
import utils.Helper.{DateTimeExtended, StringExtended}

@Singleton
class InviteController @Inject()(
                                  authUtils: AuthUtils,
                                  groupService: GroupService,
                                  inviteService: InviteService,
                                  userService: UserService
                                ) extends InjectedController {

  def create = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    (for {
      groupId <- (json \ "group_id").asOpt[Int]
      firstName <- (json \ "first_name").asOpt[String]
      lastName <- (json \ "last_name").asOpt[String]
    } yield {
      val uuid = randomUUID().toString
      val position = (json \ "position").asOpt[String]
      val surname = (json \ "surname").asOpt[String]
      groupService.findById(groupId).collect { case group =>
        val invite = Invite(firstName, surname, lastName, position, uuid, group.id, user.id)
        if (inviteService.create(invite).wasAcknowledged()) {
          Ok(Json.obj("invite_id" -> uuid))
        } else {
          NotImplemented
        }
      }.getOrElse(BadRequest)
    }).getOrElse(BadRequest)
  }

  def one(uuid: String) = Action {
    inviteService.find(uuid).map { invite =>
      val groupNameO = groupService.findById(invite.groupId).map(_.name)
      Ok(Json.toJson(invite)(Invite.writesWithGroupName(groupNameO)))
    }.getOrElse(BadRequest)
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
        invite.surname,
        invite.lastName,
        invite.position,
        Some(invite.groupId),
        password.md5.md5.md5,
        login,
        active = false,
        DateTime.now.timestamp,
        id,
        avatar = None)
      if (userService.save(user).wasAcknowledged() && inviteService.remove(invite).wasAcknowledged()) {
        Ok
      } else {
        Conflict
      }
    }).getOrElse(BadRequest)
  }
}
