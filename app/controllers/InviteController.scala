package controllers

import java.util.UUID.randomUUID

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import models.{Invite, User}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.mvc.InjectedController
import services._
import utils.Helper.{DateTimeExtended, StringExtended}

@Singleton
class InviteController @Inject()(
  authUtils: AuthUtils,
  groupService: GroupService,
  inviteService: InviteService,
  roleService: RoleService,
  userService: UserService
) extends InjectedController {

  def create(groupIdO: Option[Int]) = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    val user = request.user

    lazy val groups = groupService.all.sortBy(_.id)

    lazy val owningGroups = groups.filter(_.owner == user._id)

    lazy val groupO = groupIdO.flatMap { groupId =>
      groups.find(_.id == groupId)
    }

    (for {
      groupId <- user.groupId
      group <- groupO.orElse(owningGroups.headOption).orElse(groups.find(_.id == groupId))
      firstName <- (json \ "first_name").asOpt[String]
      lastName <- (json \ "last_name").asOpt[String]
    } yield {
      val uuid = randomUUID().toString

      val roleIdO = for {
        roleId <- (json \ "role_id").asOpt[Int]
        groupId <- groupIdO
        role <- roleService.find(roleId, groupId)
      } yield role.id

      val invite = Invite(firstName, lastName, uuid, group.id, user.id, roleIdO)
      if (inviteService.create(invite).wasAcknowledged()) {
        Ok(Json.obj("invite_id" -> uuid))
      } else {
        NotImplemented
      }
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
        invite.lastName,
        Some(invite.groupId),
        password.md5.md5.md5,
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
