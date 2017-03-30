name := "pollpush"

organization := "gov.uk.justice.digital"

version := "0.1"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "org.json4s" %% "json4s-native" % "3.5.1",
  "org.clapper" %% "grizzled-slf4j" % "1.3.0",
  "net.codingwell" %% "scala-guice" % "4.1.0",
  "com.typesafe.akka" %% "akka-http" % "10.0.5",

  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.github.tomakehurst" % "wiremock" % "2.5.1" % "test"
)

assemblyJarName in assembly := "pollPush.jar"
