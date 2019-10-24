name := "stash4s"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.0.0",
  "org.typelevel" %% "cats-effect" % "2.0.0",
  "org.typelevel" %% "cats-mtl-core" % "0.7.0",
  "co.fs2" %% "fs2-core" % "2.0.1",
  "co.fs2" %% "fs2-io" % "2.0.1",
  "org.scodec" %% "scodec-core" % "1.11.4",
  "org.scalacheck" %% "scalacheck" % "1.14.2" % "test"
)
