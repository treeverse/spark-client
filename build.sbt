name := "lakefs-spark-client"
scalaVersion := "2.12.10"

lazy val core = (project in file("core"))
  .settings(
    Compile / PB.includePaths += (Compile / resourceDirectory).value,
    Compile / PB.protoSources += (Compile / resourceDirectory).value,
    Compile / PB.targets := Seq(
      PB.gens.java -> (Compile / sourceManaged).value
    ),
    assemblySettings,
  )
lazy val examples = (project in file("examples")).dependsOn(core)
  .settings(
    assemblySettings,
  )

// Use an older JDK to be Spark compatible
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
scalacOptions ++= Seq("-release", "8", "-target:jvm-1.8")

core / libraryDependencies ++= Seq("org.rocksdb" % "rocksdbjni" % "6.6.4",
  "commons-codec" % "commons-codec" % "1.15",
  "org.apache.spark" %% "spark-sql" % "3.0.1" % "provided",
  "com.thesamet.scalapb" %% "sparksql-scalapb" % "0.10.4" % "protobuf",
  "com.thesamet.scalapb" %% "scalapb-runtime" % "0.10.4" % "protobuf",
  "org.apache.hadoop" % "hadoop-aws" % "2.10.1",
  "org.apache.hadoop" % "hadoop-common" % "2.10.1",
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "org.json4s" %% "json4s-native" % "3.7.0-M8",
  "com.google.guava" % "guava" % "30.1-jre",
  "com.google.guava" % "failureaccess" % "1.0.1",
)

examples / libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % "3.0.1" % "provided",
)

lazy val assemblySettings = Seq(
  assembly / assemblyMergeStrategy := (_ => MergeStrategy.first),
  assembly / assemblyShadeRules := Seq(
    ShadeRule.rename("org.apache.http.**" -> "org.apache.httpShaded@1").inAll,
    ShadeRule.rename("com.google.protobuf.**" -> "shadeproto.@1").inAll,
    ShadeRule.rename("com.google.common.**" -> "shadegooglecommon.@1")
      .inLibrary("com.google.guava" % "guava" % "30.1-jre", "com.google.guava" % "failureaccess" % "1.0.1")
      .inProject,
    ShadeRule.rename("scala.collection.compat.**" -> "shadecompat.@1").inAll,
  ),
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

// Currently cannot publish docs, possibly need to shade Google protobufs better
Compile / packageDoc / publishArtifact := false

publishTo := Some("Metadata Client repository" at "https://maven.pkg.github.com/treeverse/spark-client/metadata-client")
