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

  def create(name: String) = authUtils.authenticateAction { request =>
    val user = request.user
    val id = groupService.nextId
    groupService.create(Group(user, name, id))
    Ok
  }

  def list = authUtils.authenticateAction { _ =>
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
