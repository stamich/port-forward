package io.codeswarm.portforward.network.tcp

import io.codeswarm.portforward.domain.ForwardingStatus.{Running, Stopped}
import io.codeswarm.portforward.domain.{Endpoint, ForwardingConfig}
import io.codeswarm.portforward.error.{BindFailedException, ForwarderStateException}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.SystemMaterializer
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source, Tcp}
import org.apache.pekko.util.ByteString
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.net.ServerSocket
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * End-to-end tests for the Apache Pekko TCP forwarding implementation.
 */
final class TcpForwarderIntegrationSpec
    extends AnyFunSuite
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll {

  private implicit val actorSystem: ActorSystem =
    ActorSystem("tcp-forwarder-test")

  private implicit val materializer =
    SystemMaterializer(actorSystem).materializer

  private implicit val executionContext: ExecutionContext =
    actorSystem.dispatcher

  /**
   * Terminates Pekko resources after the integration suite.
   */
  override protected def afterAll(): Unit = {
    Await.result(
      actorSystem.terminate(),
      10.seconds
    )
    super.afterAll()
  }

  /** Proves bidirectional byte forwarding through a real TCP listener. */
  test("forwarder relays bytes in both directions") {
    val backendPort = freePort()
    val listenPort = freePort()

    val echoBinding =
      Await.result(
        bindEchoServer(backendPort),
        5.seconds
      )

    val forwarder =
      new TcpForwarder(
        new TcpConnectionFlowFactory
      )

    val config =
      ForwardingConfig(
        listen =
          Endpoint("127.0.0.1", listenPort),
        target =
          Endpoint("127.0.0.1", backendPort)
      )

    Await.result(
      forwarder.start(config),
      5.seconds
    )

    forwarder.status shouldBe Running

    val payload =
      ByteString("port-forward-0.2.0")

    val response =
      Await.result(
        Source
          .single(payload)
          .via(
            Tcp().outgoingConnection(
              "127.0.0.1",
              listenPort
            )
          )
          .runWith(Sink.head),
        5.seconds
      )

    response shouldBe payload

    Await.result(
      forwarder.stop(),
      5.seconds
    )

    forwarder.status shouldBe Stopped

    Await.result(
      echoBinding.unbind(),
      5.seconds
    )
  }

  /** Verifies stop is idempotent in the stopped state. */
  test("stop succeeds when forwarder is already stopped") {
    val forwarder =
      new TcpForwarder(
        new TcpConnectionFlowFactory
      )

    Await.result(
      forwarder.stop(),
      5.seconds
    ) shouldBe org.apache.pekko.Done

    forwarder.status shouldBe Stopped
  }

  /** Verifies a duplicate start request is rejected. */
  test("start rejects a second start while running") {
    val backendPort = freePort()
    val listenPort = freePort()

    val echoBinding =
      Await.result(
        bindEchoServer(backendPort),
        5.seconds
      )

    val forwarder =
      new TcpForwarder(
        new TcpConnectionFlowFactory
      )

    val config =
      ForwardingConfig(
        Endpoint("127.0.0.1", listenPort),
        Endpoint("127.0.0.1", backendPort)
      )

    Await.result(
      forwarder.start(config),
      5.seconds
    )

    val failure =
      Await.result(
        forwarder.start(config).failed,
        5.seconds
      )

    failure shouldBe a[ForwarderStateException]

    Await.result(
      forwarder.stop(),
      5.seconds
    )

    Await.result(
      echoBinding.unbind(),
      5.seconds
    )
  }

  /** Verifies bind failures restore the lifecycle to Stopped. */
  test("bind failure returns forwarder to stopped state") {
    val occupiedPort = freePort()
    val socket = new ServerSocket(occupiedPort)

    try {
      val forwarder =
        new TcpForwarder(
          new TcpConnectionFlowFactory
        )

      val config =
        ForwardingConfig(
          Endpoint(
            "127.0.0.1",
            occupiedPort
          ),
          Endpoint(
            "127.0.0.1",
            freePort()
          )
        )

      val failure =
        Await.result(
          forwarder.start(config).failed,
          5.seconds
        )

      failure shouldBe a[BindFailedException]
      forwarder.status shouldBe Stopped
    } finally {
      socket.close()
    }
  }

  /** Verifies payloads substantially larger than a single TCP chunk. */
  test("forwarder relays a large payload without corruption") {
    val backendPort = freePort()
    val listenPort = freePort()

    val echoBinding =
      Await.result(
        bindEchoServer(backendPort),
        5.seconds
      )

    val forwarder =
      new TcpForwarder(
        new TcpConnectionFlowFactory
      )

    val config =
      ForwardingConfig(
        Endpoint("127.0.0.1", listenPort),
        Endpoint("127.0.0.1", backendPort)
      )

    Await.result(
      forwarder.start(config),
      5.seconds
    )

    val payload =
      ByteString(
        Array.tabulate[Byte](1024 * 1024) { index =>
          (index % 251).toByte
        }
      )

    val response =
      Await.result(
        Source
          .single(payload)
          .via(
            Tcp().outgoingConnection(
              "127.0.0.1",
              listenPort
            )
          )
          .runFold(ByteString.empty)(_ ++ _),
        10.seconds
      )

    response shouldBe payload

    Await.result(
      forwarder.stop(),
      5.seconds
    )

    Await.result(
      echoBinding.unbind(),
      5.seconds
    )
  }

  /**
   * Starts a local TCP echo server.
   *
   * @param port local port to bind.
   * @return future Pekko TCP server binding.
   */
  private def bindEchoServer(
      port: Int
  ): Future[Tcp.ServerBinding] =
    Tcp()
      .bind("127.0.0.1", port)
      .to(
        Sink.foreach(
          _.handleWith(
            Flow[ByteString]
          )
        )
      )
      .run()

  /**
   * Reserves an unused local TCP port for test setup.
   *
   * @return currently available local port.
   */
  private def freePort(): Int = {
    val socket =
      new ServerSocket(0)

    try socket.getLocalPort
    finally socket.close()
  }
}
