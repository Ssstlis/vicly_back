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

  def create = authUtils.authenticateAction(parse.json) { request =>
    (request.body \ "name").asOpt[String].map { name =>
      val user = request.user
      val id = groupService.nextId
      if (groupService.create(Group(user, name, id)).wasAcknowledged) Ok else NotImplemented
    }.getOrElse(BadRequest)
  }

  def list = authUtils.authenticateAction {
    Ok(Json.toJson(groupService.all))
  }

  def setPurpose = authUtils.authenticateAction(parse.json) { request =>
    val json = request.body
    (for {
      id <- (json \ "id").asOpt[Int]
      purpose <- (json \ "purpose").asOpt[String]
      group <- groupService.findById(id) if group.owner.compareTo(request.user._id) == 0
    } yield {
      groupService.setPurpose(id, purpose).isUpdateOfExisting
    }).fold(BadRequest)(_ => Ok)
  }
}
