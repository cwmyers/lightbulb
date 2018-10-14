name := "lightbulb"

version := scala.util.Properties.envOrElse("APP_VERSION", "latest")

scalaVersion := "2.12.4"

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-Ywarn-unused-import",
  "-Xlint",
  "-feature",
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:higherKinds"
//    "-Xlog-implicits"
)

val unfilteredLibraryVersion = "0.9.1"

val circeVersion = "0.8.0"

resolvers += "paho" at "https://repo.eclipse.org/content/repositories/paho-releases/"

//addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)


libraryDependencies ++= Seq(
  "com.lightbend.akka" %% "akka-stream-alpakka-mqtt" % "0.20",
  "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.0.2",
  "commons-codec" % "commons-codec" % "1.11"
  )


mainClass in Compile := Some("app.Main") //Used in Universal packageBin

mainClass in (Compile, run) := Some("app.Main") //Used from normal sbt


enablePlugins(JavaServerAppPackaging, DockerPlugin)

dockerRepository := Some("192.168.1.198:5000")

dockerBaseImage := scala.util.Properties.envOrElse("DOCKER_IMAGE", "openjdk:latest")

packageName in Docker := scala.util.Properties.envOrElse("DOCKER_PACKAGE_NAME", packageName.value)

scalacOptions in (Compile, console) := Seq("without -Ywarn-unused-imports")
