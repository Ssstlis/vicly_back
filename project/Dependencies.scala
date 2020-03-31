import sbt._

object Dependencies {

  object Versions {
    val cats          = "2.1.1"
    val catsEffect    = "2.1.2"
    val catsMeowMtl   = "0.4.0"
    val catsRetry     = "1.1.0"
    val circe         = "0.13.0"
    val ciris         = "1.0.4"
    val fs2           = "2.3.0"
    val http4s        = "0.21.2"
    val http4sJwtAuth = "0.0.4"
    val newtype       = "0.4.3"
    val refined       = "0.9.13"
    val tofu          = "0.7.2.1"
    val doobie        = "0.8.8"
    val flyway        = "6.3.2"

    val bm4           = "0.3.1"
    val kindProjector = "0.11.0"
    val silencer      = "1.6.0"

    val scalaCheck    = "1.14.3"
    val scalaTest     = "3.1.1"
    val scalaTestPlus = "3.1.1.1"
  }

  object Libraries {
    def circe(artifact: String): ModuleID  = "io.circe"   %% artifact % Versions.circe
    def ciris(artifact: String): ModuleID  = "is.cir"     %% artifact % Versions.ciris
    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s

    val cats        = "org.typelevel"    %% "cats-core"     % Versions.cats
    val catsMeowMtl = "com.olegpy"       %% "meow-mtl-core" % Versions.catsMeowMtl
    val catsEffect  = "org.typelevel"    %% "cats-effect"   % Versions.catsEffect
    val catsRetry   = "com.github.cb372" %% "cats-retry"    % Versions.catsRetry
    val fs2         = "co.fs2"           %% "fs2-core"      % Versions.fs2
    val tofu        = "ru.tinkoff"       %% "tofu"          % Versions.tofu
    val tofuLogging = "ru.tinkoff"       %% "tofu-logging"  % Versions.tofu

    val doobieCore    = "org.tpolecat" %% "doobie-core"     % Versions.doobie
    val doobiePg      = "org.tpolecat" %% "doobie-postgres" % Versions.doobie
    val doobieHikari  = "org.tpolecat" %% "doobie-hikari"   % Versions.doobie
    val doobieRefined = "org.tpolecat" %% "doobie-refined"  % Versions.doobie

    val flywayCore = "org.flywaydb" % "flyway-core" % Versions.flyway

    val circeCore    = circe("circe-core")
    val circeGeneric = circe("circe-generic")
    val circeParser  = circe("circe-parser")
    val circeRefined = circe("circe-refined")

    val cirisCore    = ciris("ciris")
    val cirisEnum    = ciris("ciris-enumeratum")
    val cirisRefined = ciris("ciris-refined")

    val http4sDsl    = http4s("http4s-dsl")
    val http4sServer = http4s("http4s-blaze-server")
    val http4sClient = http4s("http4s-blaze-client")
    val http4sCirce  = http4s("http4s-circe")

    val http4sJwtAuth = "dev.profunktor" %% "http4s-jwt-auth" % Versions.http4sJwtAuth

    val refinedCore = "eu.timepit"  %% "refined"      % Versions.refined
    val refinedCats = "eu.timepit"  %% "refined-cats" % Versions.refined
    val newtype     = "io.estatico" %% "newtype"      % Versions.newtype
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
