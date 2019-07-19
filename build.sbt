val vicly_backend = project
  .in(file("."))
  .settings(
    libraryDependencies ++= Seq(
      ws,
      guice,
      "ru.tochkak" %% "play-plugins-salat" % "1.7.2",
      "com.pauldijou" %% "jwt-play-json" % "0.19.0",
      "org.typelevel" %% "cats-core" % "1.5.0",
      "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8",
      "com.sksamuel.scrimage" %% "scrimage-io-extra" % "2.1.8",
      "com.sksamuel.scrimage" %% "scrimage-filters" % "2.1.8",
      "org.apache.tika" % "tika-core" % "1.20",
      "org.apache.tika" % "tika-parsers" % "1.20",
      "jakarta.ws.rs" % "jakarta.ws.rs-api" % "2.1.4",
      "org.bytedeco" % "javacv-platform" % "1.5.1",
      "org.bytedeco" % "javacpp-presets" % "1.5.1",
      "org.bytedeco" % "ffmpeg-platform" % "4.1.3-1.5.1"
    ).map(_ exclude("javax.ws.rs", "javax.ws.rs-api")),
    name := "backend",
    version := "0.1",
    scalaVersion := "2.12.7"
  )
  .enablePlugins(PlayScala)

