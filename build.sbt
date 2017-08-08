name := "pollpush"

organization := "gov.uk.justice.digital"

version := "0.1.01"

scalaVersion := "2.12.2"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "com.pauldijou" %% "jwt-core" % "0.12.1",
  "org.json4s" %% "json4s-native" % "3.5.1",
  "org.clapper" %% "grizzled-slf4j" % "1.3.0",
  "net.codingwell" %% "scala-guice" % "4.1.0",
  "com.typesafe.akka" %% "akka-http" % "10.0.5",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.17",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.reactivemongo" %% "reactivemongo" % "0.12.1",

  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.github.tomakehurst" % "wiremock" % "2.5.1" % "test",
  "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.4" % "test"
)

assemblyJarName in assembly := "pollPush-" + version.value + ".jar"
