import Dependencies._

ThisBuild / organization := "io.codeswarm"
ThisBuild / version := "0.2.0"
ThisBuild / scalaVersion := "2.13.18"

lazy val javaFxClassifier: String =
  System.getProperty("os.name") match {
    case name if name.startsWith("Linux")   => "linux"
    case name if name.startsWith("Mac")     => "mac"
    case name if name.startsWith("Windows") => "win"
    case other                              => sys.error(s"Unsupported operating system: $other")
  }

lazy val root = (project in file("."))
  .settings(
    name := "scala-port-forward",

    Compile / mainClass :=
      Some("io.codeswarm.portforward.ForwardServerMainApplication"),

    Compile / packageBin / mainClass :=
      Some("io.codeswarm.portforward.ForwardServerMainApplication"),

    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xlint",
      "-Wvalue-discard"
    ),

    libraryDependencies ++=
      Seq(
        pekkoActor,
        pekkoStream,
        pekkoSlf4j,
        logbackClassic,
        scalaFx,
        scalaTest
      ) ++ javaFx(javaFxClassifier),

    Test / parallelExecution := false,

    assembly / mainClass :=
      Some("io.codeswarm.portforward.ForwardServerMainApplication"),

    assembly / assemblyJarName :=
      s"port-forward-server-${version.value}.jar",

    assembly / test := (Test / test).value,

    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case PathList("META-INF", _ @ _*)         => MergeStrategy.discard
      case "reference.conf"                     => MergeStrategy.concat
      case "application.conf"                   => MergeStrategy.concat
      case _                                    => MergeStrategy.first
    }
  )
