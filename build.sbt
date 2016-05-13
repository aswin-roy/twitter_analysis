import sbtassembly.Plugin.AssemblyKeys._

assemblySettings

name := "twitter_test"

version := "1.0"

scalaVersion := "2.10.4"

mainClass in assembly := Some("com.twitter.ista.appn.Application")

libraryDependencies ++= {
  Seq(
    "org.apache.spark" % "spark-streaming-twitter_2.10" % "1.6.0",
    "org.apache.spark" % "spark-core_2.10" % "1.6.0" % "provided",
    "org.apache.spark" % "spark-streaming_2.10" % "1.6.0" % "provided",
    "org.apache.spark" % "spark-mllib_2.10" % "1.6.0" % "provided",
    "com.google.code.gson" % "gson" % "1.7.1",
    "org.apache.lucene" % "lucene-core" % "5.0.0"
  )
}

mergeStrategy in assembly := {
  case PathList("org", "apache", "spark", "unused", "UnusedStubClass.class")
  => MergeStrategy.first
  case x => (mergeStrategy in assembly).value(x)
}
