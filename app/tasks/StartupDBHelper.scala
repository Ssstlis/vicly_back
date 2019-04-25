package tasks

import actors.StartupDBInitActor
import play.api.inject.{SimpleModule, _}

class StartupDBHelper extends SimpleModule(bind[StartupDBInitActor].toSelf.eagerly()) {}
