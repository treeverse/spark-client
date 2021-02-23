name := "lakefs-spark-client"
scalaVersion := "2.12.10"

Compile / PB.includePaths += (Compile / resourceDirectory).value
Compile / PB.protoSources += (Compile / resourceDirectory).value

Compile / PB.targets := Seq(
  PB.gens.java -> (Compile / sourceManaged).value
)

libraryDependencies += "org.rocksdb" % "rocksdbjni" % "6.6.4"
libraryDependencies += "commons-codec" % "commons-codec" % "1.15"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.0.1"
libraryDependencies +=  "com.google.protobuf" % "protobuf-java" % "3.14.0" % "protobuf"
libraryDependencies += "org.apache.hadoop" % "hadoop-aws" % "2.7.3"
libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.4.2"
libraryDependencies += "org.json4s" %% "json4s-native" % "3.7.0-M8"
libraryDependencies += "com.google.guava" % "guava" % "30.1-jre"

// Use an older JDK to be Spark compatible
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
scalacOptions ++= Seq("-release", "8", "-target:jvm-1.8")

assemblyShadeRules in assembly := Seq(
  ShadeRule.rename("org.apache.http.**" -> "org.apache.httpShaded@1").inAll
)

// Set credentials in this file to be able to publish from your machine.
//
// It should contain these lines (unindented):
//    realm=GitHub Package Registry
//    host=maven.pkg.github.com
//    user=YOUR-GITHUB-USERNAME
//    password=Token from https://github.com/settings/tokens (NOT your password)

credentials += Credentials(Path.userHome / ".sbt" / "credentials")

ThisBuild / versionScheme := Some("early-semver")
ThisBuild / organization := "io.treeverse"
ThisBuild / version := "0.1.0-SNAPSHOT"

publishTo := Some("Metadata Client repository" at "https://maven.pkg.github.com/treeverse/spark-client/metadata-client")
