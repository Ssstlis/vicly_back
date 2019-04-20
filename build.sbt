
val vicly_backend = project
  .in(file("."))
  .settings(
    libraryDependencies ++= Seq(
      ws,
      guice,
      "ru.tochkak" %% "play-plugins-salat" % "1.7.2",
      "com.pauldijou" %% "jwt-play-json" % "0.19.0",
      "org.typelevel" %% "cats-core" % "1.5.0"
    ),
    name := "backend",
    version := "0.1",
    scalaVersion := "2.12.7"
  )
  .enablePlugins(PlayScala)
