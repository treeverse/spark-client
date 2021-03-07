name := "lakefs-spark-client"
scalaVersion := "2.12.10"

lazy val core = (project in file("core"))
  .settings(
    Compile / PB.includePaths += (Compile / resourceDirectory).value,
    Compile / PB.protoSources += (Compile / resourceDirectory).value,
    Compile / PB.targets := Seq(
      PB.gens.java -> (Compile / sourceManaged).value
    ),
    sharedSettings,
  )
lazy val examples = (project in file("examples")).dependsOn(core)
  .settings(
    sharedSettings,
  )

// Use an older JDK to be Spark compatible
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
scalacOptions ++= Seq("-release", "8", "-target:jvm-1.8")

core / libraryDependencies ++= Seq("org.rocksdb" % "rocksdbjni" % "6.6.4",
  "commons-codec" % "commons-codec" % "1.15",
  "org.apache.spark" %% "spark-sql" % "3.0.1" % "provided",
  "com.thesamet.scalapb" %% "sparksql-scalapb" % "0.10.4" % "protobuf",
  "com.thesamet.scalapb" %% "scalapb-runtime" % "0.10.4" % "protobuf",
  "org.apache.hadoop" % "hadoop-aws" % "2.7.2",
  "org.apache.hadoop" % "hadoop-common" % "2.7.2",
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "org.json4s" %% "json4s-native" % "3.7.0-M8",
  "com.google.guava" % "guava" % "16.0.1",
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

// Don't publish root project
publish / skip := true

lazy val publishSettings = Seq(
  // Currently cannot publish docs, possibly need to shade Google protobufs better
  Compile / packageDoc / publishArtifact := false,
  publishTo := {
    val nexus = "https://s01.oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  // Remove all additional repository other than Maven Central from POM
  pomIncludeRepository := { _ => false },
)

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/treeverse/spark-client"),
    "scm:git@github.com:treeverse/spark-client.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "ariels",
    name  = "Ariel Shaqed (Scolnicov)",
    email = "ariels@treeverse.io",
    url   = url("https://github.com/arielshaqed")
  ),
  Developer(
    id    = "baraktr",
    name  = "B. A.",
    email = "barak.amar@treeverse.io",
    url   = url("https://github.com/nopcoder"),
  ),
  Developer(
    id    = "ozkatz",
    name  = "Oz Katz",
    email = "oz.katz@treeverse.io",
    url   = url("https://github.com/ozkatz"),
  ),
  Developer(
    id    = "johnnyaug",
    name  = "J. A.",
    email = "yoni.augarten@treeverse.io",
    url   = url("https://github.com/johnnyaug"),
  ),
)

lazy val sharedSettings = assemblySettings ++ publishSettings
credentials ++= Seq(
  Credentials(Path.userHome / ".sbt" / "credentials"),
  Credentials(Path.userHome / ".sbt" / "sonatype_credentials"),
)

ThisBuild / versionScheme := Some("early-semver")
ThisBuild / organization := "io.treeverse"
ThisBuild / organizationName := "Treeverse Labs"
ThisBuild / organizationHomepage := Some(url("http://treeverse.io"))
ThisBuild / description := "Spark client for lakeFS object metadata."
ThisBuild / licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://github.com/treeverse/spark-client"))

ThisBuild / version := "0.1.0-SNAPSHOT"
