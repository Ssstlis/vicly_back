//val vicly_backend = project
//  .in(file("old"))
//  .settings(
//    libraryDependencies ++= Seq(
//      ws,
//      guice,
//      "ru.tochkak" %% "play-plugins-salat" % "1.7.2",
//      "com.pauldijou" %% "jwt-play-json" % "0.19.0",
//      "org.typelevel" %% "cats-core" % "2.1.0",
//      "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8",
//      "com.sksamuel.scrimage" %% "scrimage-io-extra" % "2.1.8",
//      "com.sksamuel.scrimage" %% "scrimage-filters" % "2.1.8",
//      "org.apache.tika" % "tika-core" % "1.20",
//      "org.apache.tika" % "tika-parsers" % "1.20",
//      "jakarta.ws.rs" % "jakarta.ws.rs-api" % "2.1.4",
//      "org.bytedeco" % "javacv-platform" % "1.5.1",
//      "org.bytedeco" % "javacpp-presets" % "1.5.1",
//      "org.bytedeco" % "ffmpeg-platform" % "4.1.3-1.5.1"
//    ).map(_ exclude("javax.ws.rs", "javax.ws.rs-api")),
//    name := "backend",
//    version := "0.1",
//    scalaVersion := "2.12.10"
//  )
//  .enablePlugins(PlayScala)

val Http4sVersion = "0.21.1"
val CirceVersion = "0.13.0"
val Specs2Version = "4.8.3"
val LogbackVersion = "1.2.3"

val vicly_backend_new = project
  .in(file("new"))
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "org.specs2" %% "specs2-core" % Specs2Version % "test",
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "ru.tinkoff" %% "tofu" % "0.7.2.1"
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0"),
    scalacOptions ++= Seq(
      //      "-Ypartial-unification",
      "-deprecation",
      "-encoding", "UTF-8",
      "-language:higherKinds",
      "-language:postfixOps",
      "-feature",
      "-Xfatal-warnings",
    ),
    name := "backend_new",
    version := "0.1",
    scalaVersion := "2.13.1"
  ).enablePlugins(UniversalPlugin, DockerPlugin, JavaAppPackaging)