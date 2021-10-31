import Dependencies._
import BuildInfo._
import Dependencies.Libraries._

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val vicly_backend_new = project
  .in(file("new"))
  .settings(
    scalafmtOnCompile := true,
    baseDockerSettings,
    libraryDependencies ++= Seq(
          compilerPlugin(CompilePlugins.kindProjector cross CrossVersion.full),
          compilerPlugin(CompilePlugins.bm4),
          fs2,
          http4sJwtAuth,
          newtype,
          flywayCore,
          TestLibraries.scalaTest,
          Libraries.flywayCore
        ) ++ cats ++ tofu ++ http4s ++ doobie ++ circe ++ refined ++ pureConfig,
    name := "backend_new",
    version := "0.0.1",
    scalaVersion := "2.13.6",
  )
  .enablePlugins(UniversalPlugin, DockerPlugin, JavaAppPackaging)
  .withBuildInfo

val baseDockerSettings: Seq[Def.Setting[_]] = Seq(
  Docker / packageName:= "vicly_backend_new",
  dockerBaseImage := "openjdk:8u201-jre-alpine3.9",
  dockerExposedPorts ++= Seq(8080),
  dockerUpdateLatest := true
)
