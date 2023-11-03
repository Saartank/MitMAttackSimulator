ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.13"

lazy val root = (project in file("."))
  .settings(
    name := "MitMAttackSimulator"
  )

resolvers += "Akka Repository" at "https://repo.akka.io/releases/"
resolvers += "Maven Central" at "https://repo1.maven.org/maven2/"

libraryDependencies ++= Seq(
  // Loading JSON file
  "com.typesafe.play" %% "play-json" % "2.10.1",

  // Logging dependencies
  "ch.qos.logback" % "logback-core"    % "1.3.0-alpha10",
  "ch.qos.logback" % "logback-classic" % "1.3.0-alpha10",
  "org.slf4j"      % "slf4j-api"       % "2.0.0-alpha5",

  // Spark
  "org.apache.spark" %% "spark-core"   % "3.4.1" ,

  // Typesafe Config dependency
  "com.typesafe" % "config" % "1.4.1",

  // YAML parser dependency
  "org.yaml" % "snakeyaml" % "1.29",

  // AWS SDK for S3
  "software.amazon.awssdk" % "s3" % "2.17.52",
  "software.amazon.awssdk" % "apache-client" % "2.17.52",

  //Test
  "org.scalatest" %% "scalatest" % "3.2.10" % Test
)

excludeFilter in assembly := "META-INF/license/*"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}