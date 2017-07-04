name := """play-reactive-web-sockets"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

resolvers += Resolver.mavenLocal



libraryDependencies ++= Seq(ws,
  "org.mongodb" % "mongo-java-driver" % "3.3.0",
  "redis.clients" % "jedis" % "2.9.0",
  "com.crawler" % "com.crawler" % "1.0",
  "com.typesafe.akka" % "akka-stream-kafka_2.11" % "0.16"
)


// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
