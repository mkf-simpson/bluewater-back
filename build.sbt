import sbt._

name := "bluewater-back"

scalaVersion := "2.12.7"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-Ypartial-unification"
)

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("public")
)

libraryDependencies ++= Dependencies.projectDeps

cancelable in Global := true

