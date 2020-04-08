import sbt._

object Dependencies {

  object Versions {
    val cats          = "2.1.1"
    val catsEffect    = "2.1.2"
    val catsMeowMtl   = "0.4.0"
    val catsRetry     = "1.1.0"
    val circe         = "0.13.0"
    val fs2           = "2.3.0"
    val http4s        = "0.21.2"
    val http4sJwtAuth = "0.0.4"
    val newtype       = "0.4.3"
    val refined       = "0.9.13"
    val tofu          = "0.7.2.1"
    val doobie        = "0.8.8"
    val flyway        = "6.3.2"
    val pureconfig    = "0.12.3"

    val bm4           = "0.3.1"
    val kindProjector = "0.11.0"
    val silencer      = "1.6.0"

    val scalaCheck    = "1.14.3"
    val scalaTest     = "3.1.1"
    val scalaTestPlus = "3.1.1.1"
  }

  object Libraries {
    private val depLinker: (String => ModuleID) => Seq[String] => Seq[ModuleID] = f => _.map(f)

    private val circeL: String => ModuleID      = s => "io.circe"              %% s"circe$s"      % Versions.circe
    private val doobieL: String => ModuleID     = s => "org.tpolecat"          %% s"doobie$s"     % Versions.doobie
    private val http4sL: String => ModuleID     = s => "org.http4s"            %% s"http4s$s"     % Versions.http4s
    private val tofuL: String => ModuleID       = s => "ru.tinkoff"            %% s"tofu$s"       % Versions.tofu
    private val refinedL: String => ModuleID    = s => "eu.timepit"            %% s"refined$s"    % Versions.refined
    private val pureConfigL: String => ModuleID = s => "com.github.pureconfig" %% s"pureconfig$s" % Versions.pureconfig

    val fs2        = "co.fs2"       %% "fs2-core"   % Versions.fs2
    val flywayCore = "org.flywaydb" % "flyway-core" % Versions.flyway

    val cats = Seq(
      "org.typelevel"    %% "cats-core"     % Versions.cats,
      "com.olegpy"       %% "meow-mtl-core" % Versions.catsMeowMtl,
      "org.typelevel"    %% "cats-effect"   % Versions.catsEffect,
      "com.github.cb372" %% "cats-retry"    % Versions.catsRetry
    )

    val tofu       = depLinker(tofuL)(Seq("-core", "-logging", "-env"))
    val doobie     = depLinker(doobieL)(Seq("-core", "-postgres", "-hikari", "-refined", "-postgres-circe"))
    val circe      = depLinker(circeL)(Seq("-core", "-generic", "-parser", "-refined"))
    val http4s     = depLinker(http4sL)(Seq("-dsl", "-blaze-server", "-blaze-client", "-circe"))
    val refined    = depLinker(refinedL)(Seq("", "-cats"))
    val pureConfig = depLinker(pureConfigL)(Seq("", "-cats-effect"))

    val http4sJwtAuth = "dev.profunktor" %% "http4s-jwt-auth" % Versions.http4sJwtAuth

    val newtype = "io.estatico" %% "newtype" % Versions.newtype
  }

  object TestLibraries {
    val scalaCheck    = "org.scalacheck"    %% "scalacheck"      % Versions.scalaCheck
    val scalaTest     = "org.scalatest"     %% "scalatest"       % Versions.scalaTest
    val scalaTestPlus = "org.scalatestplus" %% "scalacheck-1-14" % Versions.scalaTestPlus
  }

  object CompilePlugins {
    val bm4           = "com.olegpy"      %% "better-monadic-for" % Versions.bm4
    val kindProjector = "org.typelevel"   % "kind-projector"      % Versions.kindProjector
    val silencer      = "com.github.ghik" % "silencer-plugin"     % Versions.silencer cross CrossVersion.full
    val silencerLib   = "com.github.ghik" % "silencer-lib"        % Versions.silencer % Provided cross CrossVersion.full
  }
}
