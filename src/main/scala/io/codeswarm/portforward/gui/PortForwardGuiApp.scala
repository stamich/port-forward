package io.codeswarm.portforward.gui

import scalafx.application.JFXApp3
import scalafx.application.Platform
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout.{GridPane, HBox, VBox}
import io.codeswarm.portforward.model.ForwardingConfig
import io.codeswarm.portforward.server.PortForwardServer
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.CoordinatedShutdown

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object PortForwardGuiApp extends JFXApp3 {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "PortForwardSystem")
  private var activeServer: Option[PortForwardServer] = None

  def start(): Unit = {
    stage = new JFXApp3.PrimaryStage {
      title = "Port Forwarding Tool"
      scene = new Scene(600, 400) {
        root = createMainUI()
      }

      onCloseRequest = _ => {
        activeServer.foreach(server =>
          server.stop().onComplete(_ => shutdownSystem())
        )

        if (activeServer.isEmpty) {
          shutdownSystem()
        }
      }
    }
  }

  private def shutdownSystem(): Unit = {
    CoordinatedShutdown(system).run(CoordinatedShutdown.JvmExitReason)
  }

  private def createMainUI() = {
    val localHostField = new TextField {
      promptText = "Local Host"
      text = "type your local host"
    }
    val localPortField = new TextField {
      promptText = "Local Port"
      text = "type your local port"
    }
    val remoteHostField = new TextField {
      promptText = "Remote Host"
      text = "type your remote host"
    }
    val remotePortField = new TextField {
      promptText = "Remote Port"
      text = "type your remote port"
    }
    val statusLabel = new Label("Ready")

    val startButton = new Button("Start Forwarding")
    val stopButton = new Button("Stop Forwarding") {
      disable = true
    }

    startButton.onAction = { _ =>
      try {
        val config = ForwardingConfig(
          localHostField.text.value,
          localPortField.text.value.toInt,
          remoteHostField.text.value,
          remotePortField.text.value.toInt
        )

        val server = new PortForwardServer(config)
        server.start().onComplete {
          case Success(_) => Platform.runLater {
            statusLabel.text = s"Forwarding from ${config.localHost}:${config.localPort} to ${config.remoteHost}:${config.remotePort}"
            startButton.disable = true
            stopButton.disable = false
            activeServer = Some(server)
          }
          case Failure(ex) => Platform.runLater {
            statusLabel.text = s"Failed to start: ${ex.getMessage}"
          }
        }
      } catch {
        case ex: Exception =>
          statusLabel.text = s"Invalid configuration: ${ex.getMessage}"
      }
    }

    stopButton.onAction = { _ =>
      activeServer.foreach { server =>
        server.stop().onComplete {
          case Success(_) => Platform.runLater {
            statusLabel.text = "Forwarding stopped"
            startButton.disable = false
            stopButton.disable = true
            activeServer = None
          }
          case Failure(ex) => Platform.runLater {
            statusLabel.text = s"Failed to stop: ${ex.getMessage}"
          }
        }
      }
    }

    val grid = new GridPane {
      hgap = 10
      vgap = 10
      padding = Insets(20)

      add(new Label("Local Host:"), 0, 0)
      add(localHostField, 1, 0)
      add(new Label("Local Port:"), 0, 1)
      add(localPortField, 1, 1)
      add(new Label("Remote Host:"), 0, 2)
      add(remoteHostField, 1, 2)
      add(new Label("Remote Port:"), 0, 3)
      add(remotePortField, 1, 3)
    }

    val buttonBox = new HBox(10) {
      alignment = Pos.Center
      children = Seq(startButton, stopButton)
    }

    new VBox(20) {
      padding = Insets(20)
      children = Seq(
        new Label("Port Forwarding Configuration") { style = "-fx-font-size: 16pt" },
        grid,
        buttonBox,
        statusLabel
      )
    }
  }
}
