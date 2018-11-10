package daos

import javax.inject._

import play.api.Environment
import salat.{Context, StringTypeHintStrategy, TypeHintFrequency}

@Singleton
class MongoContext @Inject()(environment: Environment) {

  implicit val context = {
    val context = new Context {
      val name = "global"
      override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.WhenNecessary, typeHint = "_t")
    }
    context.registerClassLoader(environment.classLoader)
    context
  }
}