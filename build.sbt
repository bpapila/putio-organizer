name := "PutioOrganizer"

version := "0.1"

scalaVersion := "2.12.8"

val akkaHttpVersion = "10.1.7"
val akkaVersion = "2.5.21"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,

  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,

  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,

  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.mockito" % "mockito-core" % "2.25.1" % Test,

  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "ch.qos.logback.contrib" % "logback-json-classic" % "0.1.5",
  "ch.qos.logback.contrib" % "logback-jackson" % "0.1.5",

)