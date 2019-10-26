name := "stash4s"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.13.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:implicitConversions",
  "-Ymacro-annotations"
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.0.0",
  "org.typelevel" %% "cats-effect" % "2.0.0",
  "org.typelevel" %% "cats-mtl-core" % "0.7.0",
  "co.fs2" %% "fs2-core" % "2.0.1",
  "co.fs2" %% "fs2-io" % "2.0.1",
  "org.scodec" %% "scodec-core" % "1.11.4",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "org.scalacheck" %% "scalacheck" % "1.14.2" % "test"
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")