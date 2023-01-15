name := "public-gateway"

version := "1.0"

lazy val `public-gateway` = (project in file("."))
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLogback)

resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"

scalaVersion := "2.13.5"

libraryDependencies ++= Seq(
  jdbc,
  ehcache,
  ws,
  evolutions,
  //Cats
  "org.typelevel" %% "cats-core"   % "2.7.0",
  "org.typelevel" %% "cats-effect" % "3.3.4",
  // App sdk
  "io.fitcentive" %% "app-sdk"          % "1.0.0",
  "io.fitcentive" %% "message-registry" % "1.0.0",
  //Logging
  "ch.qos.logback"       % "logback-classic"          % "1.3.0-alpha10",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.0.1",
  specs2                 % Test,
  guice
)

dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-core"        % "2.11.4",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.11.4",
  "com.fasterxml.jackson.core" % "jackson-databind"    % "2.11.4",
)

Universal / javaOptions ++= Seq("-Dpidfile.path=/dev/null")
