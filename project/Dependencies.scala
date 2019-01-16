import sbt._

object Dependencies {

  val LogbackV = "1.2.3"
  val CirceV = "0.11.0"
  val FinchV = "0.26.1"
  val DoobieV = "0.6.0"
  val PureConfigV = "0.10.1"
  val AkkaV = "2.5.19"
  val Http4sV = "0.20.0-M4" 

  val logback: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic" % LogbackV,
    "ch.qos.logback" % "logback-core" % LogbackV,
    "org.slf4j" % "jul-to-slf4j" % "1.7.25"
  )

  val circe: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-generic" % CirceV,
    "io.circe" %% "circe-parser" % CirceV,
    "io.circe" %% "circe-generic-extras" % CirceV,
    "io.circe" %% "circe-shapes" % CirceV
  )

  val finch: Seq[ModuleID] = Seq(
    "com.github.finagle" %% "finchx-core" % FinchV,
    "com.github.finagle" %% "finchx-circe" % FinchV
  )

  val grpc: Seq[ModuleID] = Seq(
    "io.grpc" % "grpc-netty" % "1.16.1",
    "io.hydrosphere" %% "serving-grpc-scala" % "0.2.0"
  )

  val pureconfig: Seq[ModuleID] = Seq(
    "com.github.pureconfig" %% "pureconfig" % PureConfigV
  )

  val cats: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-core" % "1.5.0",
    "org.typelevel" %% "cats-effect" % "1.1.0"
  )

  val enumeratum: Seq[ModuleID] = Seq(
    "com.beachape" %% "enumeratum" % "1.5.13",
    "com.beachape" %% "enumeratum-circe" % "1.5.19"
  )
  
  val http4s: Seq[ModuleID] = Seq(
    "org.http4s" %% "http4s-dsl" % Http4sV,
    "org.http4s" %% "http4s-blaze-client" % Http4sV,
    "org.http4s" %% "http4s-circe" % Http4sV
  )
  
  val slack: ModuleID = "com.github.seratch" % "jslack" % "1.1.6"

  val projectDeps: Seq[ModuleID] =
    logback ++ circe ++ finch ++ grpc ++ pureconfig ++ cats ++ enumeratum ++ http4s :+ slack
}

