package controllers

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import models.Group
import play.api.libs.json.Json
import play.api.mvc.InjectedController
import services.GroupService

@Singleton
class GroupController @Inject()(
                                 authUtils: AuthUtils,
                                 groupService: GroupService
                               ) extends InjectedController {

  /**
    * @api {POST} /api/group/create  Create workgroup
    * @apiName Create workgroup
    * @apiGroup Workgroups
    * @apiParam {String}        name Title of new workgoup.
    * @apiParamExample {json} Request-body:
    *                  {
    *                  "name":"New Workgroup!!!"
    *                  }
    * @apiDescription Create new workgroup with provided name in same group with user, which make request.
    */
  def create = authUtils.authenticateAction(parse.json) { request =>
    (request.body \ "name").asOpt[String].map { name =>
      val user = request.user
      val id = groupService.nextId
      if (groupService.create(Group(user, name, id)).wasAcknowledged) Ok else NotImplemented
    }.getOrElse(BadRequest)
  }

  /**
    * @api {POST} /api/group/list  Return all groups
    * @apiName Return all groups
    * @apiGroup Workgroups
    * @apiSuccessExample {json} Success-Response:
    *        [
    *                   {
    *                    "id": 1,
    *                    "name": "test",
    *                    "created": 1542470490,
    *                    "owner": "5c55d1c12cf1d52f70fb15d7",
    *                    "purpose": "Test purpose"
    *                   },
    *                   {
    *                    "id": 2,
    *                    "name": "Бухгалтерия",
    *                    "created": 1542557813,
    *                    "owner": "5c55d1c12cf1d52f70fb15d7",
    *                    "purpose": null
    *                   }
    *        ]
    * @apiDescription Create new workgroup with provided name in same group with user, which make request.
    */
  def list = authUtils.authenticateAction {
    Ok(Json.toJson(groupService.all))
  }

  /**
    * @api {POST} /api/group/set_purpose  Set purpose for group
    * @apiName Set purpose for group
    * @apiGroup Workgroups
    * @apiParam {Int}           group_id  Group id for change purpose.
    * @apiParam {String}        purpose   New purpose for group.
    * @apiParamExample {json} Request-body:
    *                  {
    *                  "chat_id":"1",
    *                  "purpose":"Frontend developers"
    *                  }
    * @apiDescription Set new purpose for workgroup.
    */
  def setPurpose = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    (for {
      groupId <- (json \ "group_id").asOpt[Int]
      purpose <- (json \ "purpose").asOpt[String]
      group <- groupService.findById(groupId) if group.owner.compareTo(request.user._id) == 0
    } yield {
      groupService.setPurpose(groupId, purpose).isUpdateOfExisting
    }).fold(BadRequest)(_ => Ok)
  }
}
