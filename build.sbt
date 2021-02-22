name := "lakefs-spark-client"
scalaVersion := "2.12.10"

organization := "io.treeverse"
version := "1.0"
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

assemblyShadeRules in assembly := Seq(
  ShadeRule.rename("org.apache.http.**" -> "org.apache.httpShaded@1").inAll
)
