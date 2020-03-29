import Dependencies._

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val vicly_backend_new = project
  .in(file("new"))
  .settings(
    scalafmtOnCompile := true,
    baseDockerSettings,
    libraryDependencies ++= Seq(
          compilerPlugin(CompilePlugins.kindProjector cross CrossVersion.full),
          compilerPlugin(CompilePlugins.betterMonadicFor),
          Libraries.cats,
          Libraries.catsEffect,
          Libraries.catsMeowMtl,
          Libraries.catsRetry,
          Libraries.tofu,
          Libraries.circeCore,
          Libraries.circeGeneric,
          Libraries.circeParser,
          Libraries.circeRefined,
          Libraries.cirisCore,
          Libraries.cirisEnum,
          Libraries.cirisRefined,
          Libraries.fs2,
          Libraries.http4sDsl,
          Libraries.http4sServer,
          Libraries.http4sClient,
          Libraries.http4sCirce,
          Libraries.http4sJwtAuth,
          Libraries.newtype,
          Libraries.refinedCore,
          Libraries.refinedCats,
          Libraries.doobieCore,
          Libraries.doobiePg,
          Libraries.doobieHikari,
          Libraries.doobieRefined,
          Libraries.flywayCore,
          Libraries.log4cats
        ),
    name := "backend_new",
    version := "0.1",
    scalaVersion := "2.13.1"
  )
  .enablePlugins(UniversalPlugin, DockerPlugin, JavaAppPackaging)

val baseDockerSettings: Seq[Def.Setting[_]] = Seq(
  packageName in Docker := "vicly_backend_new",
  dockerBaseImage := "openjdk:8u201-jre-alpine3.9",
  dockerExposedPorts ++= Seq(8080),
  dockerUpdateLatest := true
)
