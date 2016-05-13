import sbtassembly.Plugin.AssemblyKeys._

assemblySettings

name := "twitter_analysis"

version := "1.0"

scalaVersion := "2.10.4"

mainClass in assembly := Some("com.zerokool.twitter_analysis.appn.Application")

libraryDependencies ++= {
  Seq(
    "org.apache.spark" % "spark-streaming-twitter_2.10" % "1.6.0",
    "org.apache.spark" % "spark-core_2.10" % "1.6.0" % "provided",
    "org.apache.spark" % "spark-streaming_2.10" % "1.6.0" % "provided",
    "org.apache.spark" % "spark-mllib_2.10" % "1.6.0" % "provided"
  )
}

mergeStrategy in assembly := {
  case PathList("org", "apache", "spark", "unused", "UnusedStubClass.class")
  => MergeStrategy.first
  case x => (mergeStrategy in assembly).value(x)
}
