package io.codeswarm.portforward.network

import io.codeswarm.portforward.domain.{ForwardingConfig, ForwardingStatus}
import org.apache.pekko.Done

import scala.concurrent.Future

/**
 * Transport lifecycle abstraction used by the application layer.
 *
 * Presentation and orchestration code depends on this trait instead of Pekko
 * TCP implementation details.
 */
trait Forwarder {

  /**
   * Starts forwarding according to the supplied configuration.
   *
   * @param config validated forwarding rule.
   * @return future completed after the listener is successfully bound.
   */
  def start(config: ForwardingConfig): Future[Done]

  /**
   * Stops accepting new client connections.
   *
   * The method is idempotent when the forwarder is already stopped.
   *
   * @return future completed after the listener is unbound.
   */
  def stop(): Future[Done]

  /**
   * Returns the current public lifecycle state.
   *
   * @return current forwarding state.
   */
  def status: ForwardingStatus
}
