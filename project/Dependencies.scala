import sbt.*

/**
 * Central definition of external project dependencies.
 *
 * Keeping versions and module coordinates in one place makes dependency
 * upgrades explicit and prevents duplicated version literals in build.sbt.
 */
object Dependencies {

  /** Versions of third-party libraries used by the application. */
  object Versions {
    val Pekko = "1.7.0"
    val ScalaFx = "22.0.0-R33"
    val JavaFx = "22"
    val ScalaTest = "3.2.19"
    val Logback = "1.5.18"
  }

  /** Apache Pekko classic ActorSystem dependency used by Pekko Streams. */
  val pekkoActor: ModuleID =
    "org.apache.pekko" %% "pekko-actor" % Versions.Pekko

  /** Apache Pekko Streams dependency providing TCP stream integration. */
  val pekkoStream: ModuleID =
    "org.apache.pekko" %% "pekko-stream" % Versions.Pekko

  /** SLF4J bridge for Apache Pekko logging. */
  val pekkoSlf4j: ModuleID =
    "org.apache.pekko" %% "pekko-slf4j" % Versions.Pekko

  /** SLF4J implementation used by the application. */
  val logbackClassic: ModuleID =
    "ch.qos.logback" % "logback-classic" % Versions.Logback

  /** ScalaFX desktop UI dependency. */
  val scalaFx: ModuleID =
    "org.scalafx" %% "scalafx" % Versions.ScalaFx

  /** ScalaTest dependency used by unit and integration tests. */
  val scalaTest: ModuleID =
    "org.scalatest" %% "scalatest" % Versions.ScalaTest % Test

  /**
   * Returns JavaFX dependencies for the current operating system.
   *
   * @param classifier JavaFX platform classifier: linux, mac or win.
   * @return JavaFX modules required by the ScalaFX GUI.
   */
  def javaFx(classifier: String): Seq[ModuleID] =
    Seq("base", "controls", "graphics")
      .map(module => ("org.openjfx" % s"javafx-$module" % Versions.JavaFx).classifier(classifier))
}
