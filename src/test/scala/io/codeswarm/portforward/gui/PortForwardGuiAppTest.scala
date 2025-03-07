package io.codeswarm.portforward.gui

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import io.codeswarm.portforward.model.ForwardingConfig
import io.codeswarm.portforward.server.PortForwardServer
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.{ExecutionContextExecutor, Future}

class PortForwardGuiAppTest extends AnyFunSuite with Matchers with MockitoSugar with BeforeAndAfterEach {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "TestSystem")
  implicit val ec: ExecutionContextExecutor = system.executionContext

  test("should create valid configuration from UI fields") {
    val testableApp = new TestableGuiApp()

    testableApp.localHostField = "localhost"
    testableApp.localPortField = "8080"
    testableApp.remoteHostField = "example.com"
    testableApp.remotePortField = "9090"

    val config = testableApp.createConfiguration()

    config should be(ForwardingConfig("localhost", 8080, "example.com", 9090))
  }

  test("should start server with valid configuration") {
    val mockServer = mock[PortForwardServer]
    when(mockServer.start()).thenReturn(Future.successful(Done))

    val testableApp = new TestableGuiApp(serverFactory = _ => mockServer)

    testableApp.localHostField = "localhost"
    testableApp.localPortField = "8080"
    testableApp.remoteHostField = "example.com"
    testableApp.remotePortField = "9090"

    testableApp.startForwarding()

    verify(mockServer).start()
    testableApp.activeServer shouldBe defined
    testableApp.startButtonDisabled should be(true)
    testableApp.stopButtonDisabled should be(false)
  }

  test("should stop server when stop button is clicked") {
    val mockServer = mock[PortForwardServer]
    when(mockServer.start()).thenReturn(Future.successful(Done))
    when(mockServer.stop()).thenReturn(Future.successful(Done))

    val testableApp = new TestableGuiApp(serverFactory = _ => mockServer)

    testableApp.localHostField = "localhost"
    testableApp.localPortField = "8080"
    testableApp.remoteHostField = "example.com"
    testableApp.remotePortField = "9090"

    testableApp.startForwarding()
    testableApp.stopForwarding()

    verify(mockServer).stop()
    testableApp.activeServer should be(None)
    testableApp.startButtonDisabled should be(false)
    testableApp.stopButtonDisabled should be(true)
  }

  class TestableGuiApp(
                        serverFactory: ForwardingConfig => PortForwardServer = config => mock[PortForwardServer]
                      ) {
    var localHostField = ""
    var localPortField = ""
    var remoteHostField = ""
    var remotePortField = ""
    var statusText = "Ready"
    var startButtonDisabled = false
    var stopButtonDisabled = true
    var activeServer: Option[PortForwardServer] = None

    def createConfiguration(): ForwardingConfig = {
      ForwardingConfig(
        localHostField,
        localPortField.toInt,
        remoteHostField,
        remotePortField.toInt
      )
    }

    def startForwarding(): Unit = {
      try {
        val config = createConfiguration()
        val server = serverFactory(config)
        activeServer = Some(server)
        server.start()
        statusText = s"Forwarding from ${config.localHost}:${config.localPort} to ${config.remoteHost}:${config.remotePort}"
        startButtonDisabled = true
        stopButtonDisabled = false
      } catch {
        case ex: Exception =>
          statusText = s"Invalid configuration: ${ex.getMessage}"
      }
    }

    def stopForwarding(): Unit = {
      activeServer.foreach { server =>
        server.stop()
        statusText = "Forwarding stopped"
        startButtonDisabled = false
        stopButtonDisabled = true
        activeServer = None
      }
    }
  }
}
