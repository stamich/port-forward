package io.codeswarm.portforward.network

import akka.Done
import io.codeswarm.portforward.domain.{ForwardingConfig, ForwardingStatus}

import scala.concurrent.Future

/**
 * Defines the lifecycle of a transport-level forwarding implementation.
 *
 * The interface follows the Dependency Inversion Principle: presentation
 * layers depend on this abstraction instead of on Akka TCP details.
 */
trait Forwarder {

  /**
   * Starts forwarding according to the supplied rule.
   *
   * @param config validated forwarding configuration.
   * @return a future completed when the listener has been bound.
   */
  def start(config: ForwardingConfig): Future[Done]

  /**
   * Stops accepting new connections by unbinding the listener.
   *
   * Existing Akka streams are allowed to finish naturally.
   *
   * @return a future completed after the listener has been unbound.
   */
  def stop(): Future[Done]

  /**
   * Returns the current forwarding lifecycle state.
   *
   * @return current state.
   */
  def status: ForwardingStatus
}
