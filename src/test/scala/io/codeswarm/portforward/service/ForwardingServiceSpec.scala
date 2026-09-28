package io.codeswarm.portforward.service

import akka.Done
import io.codeswarm.portforward.domain.ForwardingStatus.{Running, Stopped}
import io.codeswarm.portforward.domain.{Endpoint, ForwardingConfig, ForwardingStatus}
import io.codeswarm.portforward.network.Forwarder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

/**
 * Tests orchestration behavior independently of real sockets.
 */
final class ForwardingServiceSpec extends AnyFunSuite with Matchers with ScalaFutures {

  /** In-memory forwarder used to isolate application-service tests. */
  private final class StubForwarder extends Forwarder {
    private var current: ForwardingStatus = Stopped

    /** Starts the stub. */
    override def start(config: ForwardingConfig): Future[Done] = {
      current = Running
      Future.successful(Done)
    }

    /** Stops the stub. */
    override def stop(): Future[Done] = {
      current = Stopped
      Future.successful(Done)
    }

    /** Returns current stub state. */
    override def status: ForwardingStatus = current
  }

  /** Ensures valid rules are delegated to the transport implementation. */
  test("start delegates valid configuration to forwarder") {
    val service = new ForwardingService(new StubForwarder)
    val config = ForwardingConfig(Endpoint("127.0.0.1", 9000), Endpoint("localhost", 8080))

    whenReady(service.start(config))(_ shouldBe Done)
    service.status shouldBe Running
  }

  /** Ensures invalid rules fail before the forwarder is called. */
  test("start rejects invalid configuration") {
    val service = new ForwardingService(new StubForwarder)
    val config = ForwardingConfig(Endpoint("", 0), Endpoint("localhost", 8080))

    whenReady(service.start(config).failed) { ex =>
      ex shouldBe a[IllegalArgumentException]
    }

    service.status shouldBe Stopped
  }
}
