name := "redisclient-example"

version := "0.1"

scalaVersion := "2.11.8"

val akkaVersion = "2.5.16"
val akkaHTTPVersion = "10.1.4"
val akka = "com.typesafe.akka"

libraryDependencies ++= Seq(
  akka %% "akka-actor" % akkaVersion,
  akka %% "akka-stream" % akkaVersion,
  akka %% "akka-http-core" % akkaHTTPVersion,
  akka %% "akka-http" % akkaHTTPVersion,
  akka % "akka-slf4j_2.11" % "2.4.1",
  "net.debasishg" %% "redisclient" % "3.8"
)
