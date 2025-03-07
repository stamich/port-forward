ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.16"
ThisBuild / organization := "io.codeswarm"

Compile / mainClass := Some("io.codeswarm.portforward.ForwardServerMainApplication")
Compile / packageBin / mainClass := Some("io.codeswarm.portforward.ForwardServerMainApplication")

Compile / packageOptions += Package.ManifestAttributes(
  ("Main-Class", "io.codeswarm.portforward.ForwardServerMainApplication")
)

lazy val root = (project in file("."))
  .settings(
    name := "scala-port-forward",

    // Assembly settings for fat JAR
    assembly / mainClass := Some("io.codeswarm.portforward.ForwardServerMainApplication"),
    assembly / assemblyJarName := "port-forward-server.jar",

    // Handle merge conflicts during assembly
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case "reference.conf" => MergeStrategy.concat
      case x => MergeStrategy.first
    },

    // Library dependencies
    libraryDependencies ++= {
      val osName = System.getProperty("os.name") match {
        case n if n.startsWith("Linux") => "linux"
        case n if n.startsWith("Mac") => "mac"
        case n if n.startsWith("Windows") => "win"
        case _ => throw new Exception("Unknown platform!")
      }

      Seq(
        // ScalaFX
        "org.scalafx" %% "scalafx" % "22.0.0-R33",

        // Akka
        "com.typesafe.akka" %% "akka-actor" % "2.8.8",
        "com.typesafe.akka" %% "akka-actor-typed" % "2.8.8",
        "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.8.8" % "test",
        "com.typesafe.akka" %% "akka-stream" % "2.8.8",
        "com.typesafe.akka" %% "akka-stream-testkit" % "2.8.8",
        "com.typesafe.akka" %% "akka-slf4j" % "2.8.8",
        "com.typesafe.akka" %% "akka-http" % "10.5.3",
        "com.typesafe.akka" %% "akka-http-core" % "10.5.3",

        // Cats
        "org.typelevel" %% "cats-core" % "2.13.0",
        "org.typelevel" %% "cats-testkit" % "2.13.0" % "test",
        "org.typelevel" %% "cats-effect" % "3.5.7",

        // ScalaTest
        "org.scalatest" %% "scalatest" % "3.2.19" % "test",
        "org.scalatest" %% "scalatest-funsuite" % "3.2.19" % "test",
        "org.scalatestplus" %% "mockito-4-11" % "3.2.18.0" % "test",

        "org.mockito" % "mockito-core" % "5.16.0" % "test",
      ) ++ Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
        .map(m => "org.openjfx" % s"javafx-$m" % "22" classifier osName)
    }
  )