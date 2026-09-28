package io.codeswarm.portforward.service

import akka.Done
import io.codeswarm.portforward.domain.{ForwardingConfig, ForwardingStatus}
import io.codeswarm.portforward.network.Forwarder
import io.codeswarm.portforward.validation.ConfigValidator

import scala.concurrent.Future

/**
 * Application service coordinating validation and transport lifecycle.
 *
 * @param forwarder transport implementation.
 */
final class ForwardingService(forwarder: Forwarder) {

  /**
   * Validates and starts one forwarding rule.
   *
   * @param config rule requested by the caller.
   * @return future completed when forwarding is active.
   */
  def start(config: ForwardingConfig): Future[Done] =
    ConfigValidator.validate(config) match {
      case Right(validConfig) =>
        forwarder.start(validConfig)

      case Left(errors) =>
        Future.failed(new IllegalArgumentException(errors.mkString("; ")))
    }

  /**
   * Stops forwarding.
   *
   * @return future completed after the listener has been unbound.
   */
  def stop(): Future[Done] =
    forwarder.stop()

  /**
   * Exposes the current forwarding state to presentation layers.
   *
   * @return current forwarding state.
   */
  def status: ForwardingStatus =
    forwarder.status
}
