package io.codeswarm.portforward.server

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Terminated}
import io.codeswarm.portforward.model.ForwardingConfig
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}

import java.net.{ServerSocket, Socket}
import scala.concurrent.{Await, ExecutionContextExecutor}

class PortForwardServerSpec extends AnyFlatSpec with Matchers
  with ScalaFutures with Eventually with BeforeAndAfterAll {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "TestSystem")
  implicit val ec: ExecutionContextExecutor = system.executionContext

  override def afterAll(): Unit = {
    system.whenTerminated.foreach { _ => ()}
    Thread.sleep(1000)
    super.afterAll()
  }

  "PortForwardServer" should "start and stop successfully" in {
    val localPort = findAvailablePort()
    val remotePort = findAvailablePort()

    val mockServer = new Thread(() => {
      val serverSocket = new ServerSocket(remotePort)
      try {
        val socket = serverSocket.accept()
        socket.close()
      } finally {
        serverSocket.close()
      }
    })
    mockServer.setDaemon(true)
    mockServer.start()

    val config = ForwardingConfig(
      localHost = "localhost",
      localPort = localPort,
      remoteHost = "localhost",
      remotePort = remotePort
    )

    val server = new PortForwardServer(config)
    val startFuture = server.start()

    whenReady(startFuture) { result =>
      val socket = new Socket("localhost", localPort)
      socket.isConnected shouldBe true
      socket.close()

      val stopFuture = server.stop()
      whenReady(stopFuture) { _ => succeed }
    }
  }

  private def findAvailablePort(): Int = {
    val socket = new ServerSocket(0)
    try {
      socket.getLocalPort
    } finally {
      socket.close()
    }
  }
}
