package io.codeswarm.portforward.gui

import io.codeswarm.portforward.domain.{Endpoint, ForwardingConfig}
import io.codeswarm.portforward.runtime.ApplicationRuntime
import scalafx.application.{JFXApp3, Platform}
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label, TextField}
import scalafx.scene.layout.{GridPane, VBox}

import scala.util.{Failure, Success}

/**
 * Minimal ScalaFX front end for configuring one TCP forwarding rule.
 *
 * The GUI depends only on the application service exposed by
 * [[ApplicationRuntime]]; it does not contain networking implementation logic.
 */
object PortForwardGuiApp extends JFXApp3 {

  private val runtime = ApplicationRuntime.create()

  /**
   * Creates and displays the primary JavaFX stage.
   */
  override def start(): Unit = {
    val localHostField = new TextField {
      text = "127.0.0.1"
      promptText = "Local host"
    }

    val localPortField = new TextField {
      text = "8090"
      promptText = "Local port"
    }

    val remoteHostField = new TextField {
      text = "example.com"
      promptText = "Remote host"
    }

    val remotePortField = new TextField {
      text = "80"
      promptText = "Remote port"
    }

    val statusLabel = new Label("Stopped")
    val startButton = new Button("Start")
    val stopButton = new Button("Stop") {
      disable = true
    }

    val grid = new GridPane {
      hgap = 10
      vgap = 10

      add(new Label("Local host:"), 0, 0)
      add(localHostField, 1, 0)
      add(new Label("Local port:"), 0, 1)
      add(localPortField, 1, 1)
      add(new Label("Remote host:"), 0, 2)
      add(remoteHostField, 1, 2)
      add(new Label("Remote port:"), 0, 3)
      add(remotePortField, 1, 3)
    }

    startButton.onAction = _ => {
      parseConfig(
        localHostField.text.value,
        localPortField.text.value,
        remoteHostField.text.value,
        remotePortField.text.value
      ) match {
        case Right(config) =>
          startButton.disable = true
          statusLabel.text = "Starting..."

          runtime.forwardingService.start(config).onComplete {
            case Success(_) =>
              Platform.runLater {
                statusLabel.text = s"Forwarding ${config.listen} -> ${config.target}"
                stopButton.disable = false
              }

            case Failure(ex) =>
              Platform.runLater {
                statusLabel.text = s"Start failed: ${ex.getMessage}"
                startButton.disable = false
              }
          }(runtime.actorSystem.dispatcher)

        case Left(error) =>
          statusLabel.text = error
      }
    }

    stopButton.onAction = _ => {
      stopButton.disable = true
      statusLabel.text = "Stopping..."

      runtime.forwardingService.stop().onComplete {
        case Success(_) =>
          Platform.runLater {
            statusLabel.text = "Stopped"
            startButton.disable = false
          }

        case Failure(ex) =>
          Platform.runLater {
            statusLabel.text = s"Stop failed: ${ex.getMessage}"
            startButton.disable = false
          }
      }(runtime.actorSystem.dispatcher)
    }

    stage = new JFXApp3.PrimaryStage {
      title = "Port Forward Server 0.1.1"
      scene = new Scene(520, 300) {
        root = new VBox {
          spacing = 14
          padding = Insets(20)
          children = Seq(
            grid,
            startButton,
            stopButton,
            statusLabel
          )
        }
      }

      onCloseRequest = _ => {
        runtime.shutdown()
        ()
      }
    }
  }

  /**
   * Converts GUI text fields into a forwarding configuration.
   *
   * @param localHost local bind host.
   * @param localPort local bind port.
   * @param remoteHost remote target host.
   * @param remotePort remote target port.
   * @return parsed configuration or a user-facing error.
   */
  private def parseConfig(
      localHost: String,
      localPort: String,
      remoteHost: String,
      remotePort: String
  ): Either[String, ForwardingConfig] =
    for {
      parsedLocalPort <- parsePort("Local", localPort)
      parsedRemotePort <- parsePort("Remote", remotePort)
    } yield ForwardingConfig(
      listen = Endpoint(localHost, parsedLocalPort),
      target = Endpoint(remoteHost, parsedRemotePort)
    )

  /**
   * Parses one GUI port field.
   *
   * @param name field label used in the error message.
   * @param value textual port.
   * @return numeric port or descriptive error.
   */
  private def parsePort(name: String, value: String): Either[String, Int] =
    scala.util.Try(value.toInt).toEither.left.map(_ => s"$name port must be a number")
}
