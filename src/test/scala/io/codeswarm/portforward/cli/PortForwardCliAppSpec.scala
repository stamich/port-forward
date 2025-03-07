package io.codeswarm.portforward.cli

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import io.codeswarm.portforward.model.ForwardingConfig
import io.codeswarm.portforward.server.PortForwardServer
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.{ExecutionContextExecutor, Future}

class PortForwardCliAppSpec extends AnyFunSuite with Matchers with MockitoSugar {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "TestSystem")
  implicit val ec: ExecutionContextExecutor = system.executionContext

  test("should load configuration and start server with valid arguments") {
    val mockConfig = ForwardingConfig("localhost", 8080, "remote.host", 9090)
    val mockServer = mock[PortForwardServer]
    val mockParser = mock[CommandLineParserHelper]

    when(mockParser.parse(any())).thenReturn(Some(mockConfig))
    when(mockServer.start()).thenReturn(Future.successful(Done))
    when(mockServer.stop()).thenReturn(Future.successful(Done))

    val testApp = new TestableCliApp(mockParser, mockServer)
    val args = Array("--local", "localhost:8080", "--remote", "remote.host:9090")

    testApp.runApp(args)

    verify(mockParser).parse(args)
    verify(mockServer).start()
  }

  test("should load configuration from file when no arguments provided") {
    val mockConfig = ForwardingConfig("localhost", 8080, "remote.host", 9090)
    val mockServer = mock[PortForwardServer]
    val mockParser = mock[CommandLineParserHelper]
    val mockConfigLoader = mock[ConfigFileLoaderHelper]

    when(mockParser.parse(any())).thenReturn(None)
    when(mockConfigLoader.loadFromFile()).thenReturn(Some(mockConfig))
    when(mockServer.start()).thenReturn(Future.successful(Done))
    when(mockServer.stop()).thenReturn(Future.successful(Done))

    val testApp = new TestableCliApp(mockParser, mockServer, mockConfigLoader)

    testApp.runApp(Array.empty)

    verify(mockConfigLoader).loadFromFile()
    verify(mockServer).start()
  }

  test("should terminate system when configuration is invalid") {
    val mockServer = mock[PortForwardServer]
    val mockParser = mock[CommandLineParserHelper]
    val mockConfigLoader = mock[ConfigFileLoaderHelper]

    when(mockParser.parse(any())).thenReturn(None)
    when(mockConfigLoader.loadFromFile()).thenReturn(None)

    val testApp = new TestableCliApp(mockParser, mockServer, mockConfigLoader)

    testApp.runApp(Array.empty)

    verify(mockServer, never()).start()
    testApp.systemTerminated should be(true)
  }

  class TestableCliApp(
                        mockParser: CommandLineParserHelper = mock[CommandLineParserHelper],
                        mockServer: PortForwardServer = mock[PortForwardServer],
                        mockConfigLoader: ConfigFileLoaderHelper = mock[ConfigFileLoaderHelper]
                      ) {
    var systemTerminated = false

    def runApp(args: Array[String]): Unit = {
      val config = mockParser.parse(args) match {
        case Some(config) => Some(config)
        case None if args.isEmpty =>
          mockConfigLoader.loadFromFile()
        case None if args.length == 1 =>
          mockConfigLoader.loadFromFile(args(0))
        case None => None
      }

      config match {
        case Some(c) =>
          mockServer.start()
        case None =>
          systemTerminated = true
      }
    }
  }

  trait CommandLineParserHelper {
    def parse(args: Array[String]): Option[ForwardingConfig]
  }

  trait ConfigFileLoaderHelper {
    def loadFromFile(): Option[ForwardingConfig]
    def loadFromFile(path: String): Option[ForwardingConfig]
  }
}
