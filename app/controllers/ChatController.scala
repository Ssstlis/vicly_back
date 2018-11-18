package controllers

import com.google.inject.{Inject, Singleton}
import play.api.mvc.InjectedController
import services.{ChatService, UserService}

@Singleton
class ChatController @Inject() (
  chatService: ChatService,
  userService: UserService
) extends InjectedController {

}
