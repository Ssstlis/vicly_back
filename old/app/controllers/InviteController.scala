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
                                  userService: UserService
                                ) extends InjectedController {


  /**
    * @api {POST} /api/invite  Create invite for new user
    * @apiName Create invite for new user
    * @apiGroup Invite
    * @apiParam {Int}           group_id    Group id of new user.
    * @apiParam {String}        first_name  First name of new user.
    * @apiParam {String}        last_name   Last name of new user.
    * @apiParam {String}        position    Position of new user, can be empty or not present.
    * @apiParam {String}        surname     Surname(LOL) of new user.
    * @apiParamExample {json} Request-body:
    *                  {
    *                  "group_id":"1",
    *                  "first_name":"Ivan",
    *                  "last_name" :"Ivanovich",
    *                  "position"  :"Seledka frontenda"
    *                  }
    * @apiSuccessExample {json} Success-Response:
    *     {
    *       "invite_id": "0df515d6-5eb9-4c8a-a73e-ea5f76e27601"
    *     }
    *
    * @apiDescription Create invite for new user
    */
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
        if (inviteService.create(invite).wasAcknowledged()) Ok(Json.obj("invite_id" -> uuid)) else NotImplemented
      }.getOrElse(BadRequest)
    }).getOrElse(BadRequest)
  }

  /**
    * @api {GET} /api/invite/:uuid  Get invite info. when new user will use invite.
    * @apiName Get invite info.
    * @apiGroup Invite
    * @apiParam {String}           UUID of invite.
    * @apiSuccessExample {json} Success-Response:
    *                    {
    *                    "first_name": "Ivan",
    *                    "surname": "Ivanovich",
    *                    "last_name": "Ivanovich",
    *                    "position": "Frontend dev",
    *                    "uuid": "0df515d6-5eb9-4c8a-a73e-ea5f76e27601",
    *                    "group_id": "1",
    *                    "inviter_id": "10"
    *                    }
    * @apiDescription Get invite info. when new user will use invite.
    */
  def one(uuid: String) = Action {
    inviteService.find(uuid).map { invite =>
      val groupNameO = groupService.findById(invite.groupId).map(_.name)
      Ok(Json.toJson(invite)(Invite.writesWithGroupName(groupNameO)))
    }.getOrElse(BadRequest)
  }

  /**
    * @api {GET} /api/invite/list  Get invite list.
    * @apiName Get invite list.
    * @apiGroup Invite
    * @apiSuccessExample {json} Success-Response:
    *                    [{
    *                    "first_name": "Ivan",
    *                    "surname": "Ivanovich",
    *                    "last_name": "Ivanovich",
    *                    "position": "Frontend dev",
    *                    "uuid": "0df515d6-5eb9-4c8a-a73e-ea5f76e27601",
    *                    "group_id": "1",
    *                    "inviter_id": "10"
    *                    },
    *                    {
    *                    "first_name": "Lol",
    *                    "surname": "Lolkovich",
    *                    "last_name": "Lolkovich",
    *                    "position": "Backend dev",
    *                    "uuid": "0df515d6-5eb9-4c8a-a73e-ea5f76e27603",
    *                    "group_id": "1",
    *                    "inviter_id": "10"
    *                    }]
    * @apiDescription Get invite info. when new user will use invite.
    */
  def list = Action {
    Ok(Json.toJson(inviteService.all))
  }

  /**
    * @api {POST} /api/invite/signup   Set user login and password for invite and register user
    * @apiName Set user login and pass with invite
    * @apiGroup Invite
    * @apiParam {Int}           uuid      UUID of invite.
    * @apiParam {String}        login     Login for user account.
    * @apiParam {String}        password  Password user account.
    * @apiParamExample {json} Request-body:
    *                  {
    *                  "uuid":"0df515d6-5eb9-4c8a-a73e-ea5f76e27603",
    *                  "login":"lol"
    *                  "password":"lol_password"
    *                  }
    * @apiDescription Set user login and password for invite and register user.
    */
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
      if (userService.save(user).wasAcknowledged() && inviteService.remove(invite).wasAcknowledged()) Ok else Conflict
    }).getOrElse(BadRequest)
  }
}
