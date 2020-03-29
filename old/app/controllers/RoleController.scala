package controllers

import actions.AuthUtils
import com.google.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.InjectedController
import services.{RoleService, UserService}

@Singleton
class RoleController @Inject()(
  authUtils: AuthUtils,
  roleService: RoleService,
  userService: UserService
) extends InjectedController {

  def create(description: String) = authUtils.authenticateAction { request =>
    request.user.groupId.collect { case groupId if roleService.save(description, groupId) => Ok
    }.getOrElse(BadRequest)
  }

  def update(description: String, id: Int) = authUtils.authenticateAction { request =>
    request.user.groupId.collect { case groupId
      if roleService.update(id, description, groupId).isUpdateOfExisting => Ok
    }.getOrElse(BadRequest)
  }

  def remove(id: Int) = authUtils.authenticateAction { request =>
    request.user.groupId.collect { case groupId
      if userService.roleCount(id, groupId) == 0 && roleService.remove(id).isUpdateOfExisting => Ok
    }.getOrElse(BadRequest)
  }

  def list = authUtils.authenticateAction { request =>
    request.user.groupId.map { groupId =>
      val roles = roleService.findGroupRoles(groupId)
      Ok(Json.toJson(roles))
    }.getOrElse(BadRequest)
  }

  def one(id: Int) = authUtils.authenticateAction { request =>
    request.user.groupId.flatMap(groupId =>
      roleService.find(id, groupId).map(role => Ok(Json.toJson(role)))
    ).getOrElse(BadRequest)
  }

  def oneNoAuth(id: Int) = Action {
    roleService.find(id).map(role => Ok(Json.toJson(role))).getOrElse(BadRequest)
  }
}
