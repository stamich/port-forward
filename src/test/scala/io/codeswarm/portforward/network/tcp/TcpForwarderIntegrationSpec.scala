package io.codeswarm.portforward.network.tcp

import akka.actor.ActorSystem
import akka.stream.SystemMaterializer
import akka.stream.scaladsl.{Sink, Source, Tcp}
import akka.util.ByteString
import io.codeswarm.portforward.domain.{Endpoint, ForwardingConfig}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.net.ServerSocket
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

/**
 * End-to-end test proving that bytes can traverse the TCP forwarder.
 */
final class TcpForwarderIntegrationSpec
    extends AnyFunSuite
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll {

  private implicit val actorSystem: ActorSystem = ActorSystem("tcp-forwarder-test")
  private implicit val materializer = SystemMaterializer(actorSystem).materializer
  private implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  /**
   * Terminates Akka resources after all integration tests.
   */
  override protected def afterAll(): Unit = {
    Await.result(actorSystem.terminate(), 10.seconds)
    super.afterAll()
  }

  /**
   * Proves that a payload sent to the listen endpoint reaches an echo backend
   * and returns unchanged.
   */
  test("forwarder relays bytes in both directions") {
    val backendPort = freePort()
    val listenPort = freePort()

    val echoBinding =
      Await.result(
        Tcp()
          .bind("127.0.0.1", backendPort)
          .to(Sink.foreach(_.handleWith(akka.stream.scaladsl.Flow[ByteString])))
          .run(),
        5.seconds
      )

    val forwarder = new TcpForwarder(new TcpConnectionFlowFactory)
    val config = ForwardingConfig(
      listen = Endpoint("127.0.0.1", listenPort),
      target = Endpoint("127.0.0.1", backendPort)
    )

    Await.result(forwarder.start(config), 5.seconds)

    val payload = ByteString("port-forward-0.1.1")
    val response =
      Await.result(
        Source
          .single(payload)
          .via(Tcp().outgoingConnection("127.0.0.1", listenPort))
          .runWith(Sink.head),
        5.seconds
      )

    response shouldBe payload

    Await.result(forwarder.stop(), 5.seconds)
    Await.result(echoBinding.unbind(), 5.seconds)
  }

  /**
   * Reserves a currently unused local TCP port for test setup.
   *
   * @return available local port number.
   */
  private def freePort(): Int = {
    val socket = new ServerSocket(0)
    try socket.getLocalPort
    finally socket.close()
  }
}
