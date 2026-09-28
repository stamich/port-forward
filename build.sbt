ThisBuild / organization := "io.codeswarm"
ThisBuild / version := "0.1.1"
ThisBuild / scalaVersion := "2.13.16"

lazy val akkaVersion = "2.8.8"
lazy val scalaFxVersion = "22.0.0-R33"
lazy val scalaTestVersion = "3.2.19"
lazy val logbackVersion = "1.5.18"

lazy val root = (project in file("."))
  .settings(
    name := "scala-port-forward",
    Compile / mainClass := Some("io.codeswarm.portforward.ForwardServerMainApplication"),
    Compile / packageBin / mainClass := Some("io.codeswarm.portforward.ForwardServerMainApplication"),

    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xlint"
    ),

    libraryDependencies ++= {
      val osName = System.getProperty("os.name") match {
        case n if n.startsWith("Linux")   => "linux"
        case n if n.startsWith("Mac")     => "mac"
        case n if n.startsWith("Windows") => "win"
        case other                        => sys.error(s"Unsupported operating system: $other")
      }

      Seq(
        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "com.typesafe.akka" %% "akka-stream" % akkaVersion,
        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
        "ch.qos.logback" % "logback-classic" % logbackVersion,
        "org.scalafx" %% "scalafx" % scalaFxVersion,
        "org.scalatest" %% "scalatest" % scalaTestVersion % Test
      ) ++
        Seq("base", "controls", "graphics")
          .map(module => "org.openjfx" % s"javafx-$module" % "22" classifier osName)
    },

    Test / parallelExecution := false,

    assembly / mainClass := Some("io.codeswarm.portforward.ForwardServerMainApplication"),
    assembly / assemblyJarName := "port-forward-server.jar",
    assembly / test := (Test / test).value,
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case PathList("META-INF", _ @ _*)         => MergeStrategy.discard
      case "reference.conf"                     => MergeStrategy.concat
      case "application.conf"                   => MergeStrategy.concat
      case _                                    => MergeStrategy.first
    }
  )
