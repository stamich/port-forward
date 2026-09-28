package io.codeswarm.portforward.service

import io.codeswarm.portforward.domain.ForwardingStatus.{Running, Stopped}
import io.codeswarm.portforward.domain.{Endpoint, ForwardingConfig, ForwardingStatus}
import io.codeswarm.portforward.error.InvalidConfigurationException
import io.codeswarm.portforward.network.Forwarder
import org.apache.pekko.Done
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

/**
 * Tests application orchestration independently of real sockets.
 */
final class ForwardingServiceSpec
    extends AnyFunSuite
    with Matchers
    with ScalaFutures {

  /**
   * In-memory forwarding adapter used to isolate service tests.
   */
  private final class StubForwarder extends Forwarder {
    private var current: ForwardingStatus = Stopped

    /**
     * Marks the stub as running.
     *
     * @param config ignored test configuration.
     * @return completed future.
     */
    override def start(
        config: ForwardingConfig
    ): Future[Done] = {
      current = Running
      Future.successful(Done)
    }

    /**
     * Marks the stub as stopped.
     *
     * @return completed future.
     */
    override def stop(): Future[Done] = {
      current = Stopped
      Future.successful(Done)
    }

    /**
     * Returns current stub status.
     *
     * @return current state.
     */
    override def status: ForwardingStatus =
      current
  }

  /** Ensures valid rules are delegated to the transport. */
  test("start delegates valid configuration") {
    val service =
      new ForwardingService(new StubForwarder)

    val config =
      ForwardingConfig(
        Endpoint("127.0.0.1", 9000),
        Endpoint("localhost", 8080)
      )

    whenReady(service.start(config)) {
      _ shouldBe Done
    }

    service.status shouldBe Running
  }

  /** Ensures invalid configuration fails before the transport is called. */
  test("start rejects invalid configuration") {
    val service =
      new ForwardingService(new StubForwarder)

    val config =
      ForwardingConfig(
        Endpoint("", 0),
        Endpoint("localhost", 8080)
      )

    whenReady(service.start(config).failed) {
      _ shouldBe a[InvalidConfigurationException]
    }

    service.status shouldBe Stopped
  }
}
