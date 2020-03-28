import Dependencies.Libraries

lazy val vicly_backend_new = project
  .in(file("new"))
  .settings(
    scalafmtOnCompile := true,
    packageName in Docker := "vicly_backend_new",
    dockerBaseImage := "openjdk:8u201-jre-alpine3.9",
    dockerExposedPorts ++= Seq(8080),
    dockerUpdateLatest := true,
    libraryDependencies ++= Seq(
          compilerPlugin(Libraries.kindProjector cross CrossVersion.full),
          compilerPlugin(Libraries.betterMonadicFor),
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
          Libraries.log4cats,
          Libraries.logback % Runtime,
          Libraries.newtype,
          Libraries.refinedCore,
          Libraries.refinedCats,
          Libraries.doobieCore,
          Libraries.doobiePg,
          Libraries.doobieHikari
        ),
    scalacOptions ++= Seq(
          //      "-Ypartial-unification",
          "-deprecation",
          "-explaintypes",
          "-feature",
          "-encoding",
          "UTF-8",
          "-language:higherKinds",
          "-language:postfixOps",
          "-language:implicitConversions",
          "-feature",
          "-Xfatal-warnings",
          "-Ybackend-parallelism",
          "8"
        ),
    name := "backend_new",
    version := "0.1",
    scalaVersion := "2.13.1"
  )
  .enablePlugins(UniversalPlugin, DockerPlugin, JavaAppPackaging)
