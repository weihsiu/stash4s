name := "stash4s"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.13.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:implicitConversions",
  "-Ymacro-annotations",
  "-Ywarn-unused"
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.1.0",
  "org.typelevel" %% "cats-effect" % "2.0.0",
  "org.typelevel" %% "cats-mtl-core" % "0.7.0",
  "com.olegpy" %% "meow-mtl-core" % "0.4.0",
  "com.olegpy" %% "meow-mtl-effects" % "0.4.0",
  "co.fs2" %% "fs2-core" % "2.1.0",
  "co.fs2" %% "fs2-io" % "2.1.0",
  "org.scodec" %% "scodec-core" % "1.11.4",
  "org.scodec" %% "scodec-stream" % "2.0.0",
  "com.github.julien-truffaut" %%  "monocle-core"  % "2.0.1",
  "com.github.julien-truffaut" %%  "monocle-macro" % "2.0.1",
  "org.scalatest" %% "scalatest" % "3.1.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.14.3" % "test"
)

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")