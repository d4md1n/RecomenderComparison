name := "RecomenderComparison"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.3.1" % "provided",
  "org.apache.spark" %% "spark-mllib" % "1.3.1",
    "org.scalanlp" %% "breeze" % "0.12",
    "org.scalanlp" %% "breeze-viz" % "0.12"
)
resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
resolvers += Resolver.mavenLocal