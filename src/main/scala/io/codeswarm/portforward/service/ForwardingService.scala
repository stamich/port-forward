package io.codeswarm.portforward.service

import io.codeswarm.portforward.domain.{ForwardingConfig, ForwardingStatus}
import io.codeswarm.portforward.error.InvalidConfigurationException
import io.codeswarm.portforward.network.Forwarder
import io.codeswarm.portforward.validation.ConfigValidator
import org.apache.pekko.Done

import scala.concurrent.Future

/**
 * Application service coordinating validation and forwarding lifecycle.
 *
 * @param forwarder transport implementation hidden behind the Forwarder
 *                  abstraction.
 */
final class ForwardingService(forwarder: Forwarder) {

  /**
   * Validates and starts forwarding.
   *
   * @param config requested forwarding rule.
   * @return future completed after the listener is active.
   */
  def start(config: ForwardingConfig): Future[Done] =
    ConfigValidator.validate(config) match {
      case Right(validConfig) =>
        forwarder.start(validConfig)

      case Left(errors) =>
        Future.failed(
          InvalidConfigurationException(errors)
        )
    }

  /**
   * Stops forwarding.
   *
   * @return future completed after the listener is unbound.
   */
  def stop(): Future[Done] =
    forwarder.stop()

  /**
   * Returns the current forwarding status.
   *
   * @return public lifecycle state.
   */
  def status: ForwardingStatus =
    forwarder.status
}
