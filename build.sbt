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
  "org.typelevel" %% "cats-core" % "2.0.0",
  "org.typelevel" %% "cats-effect" % "2.0.0",
  "org.typelevel" %% "cats-mtl-core" % "0.7.0",
  "com.olegpy" %% "meow-mtl-core" % "0.4.0",
  "com.olegpy" %% "meow-mtl-effects" % "0.4.0",
  "co.fs2" %% "fs2-core" % "2.0.1",
  "co.fs2" %% "fs2-io" % "2.0.1",
  "org.scodec" %% "scodec-core" % "1.11.4",
  "com.github.julien-truffaut" %%  "monocle-core"  % "2.0.0",
  "com.github.julien-truffaut" %%  "monocle-macro" % "2.0.0",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "org.scalacheck" %% "scalacheck" % "1.14.2" % "test"
)

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")