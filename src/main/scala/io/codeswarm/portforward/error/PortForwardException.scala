package io.codeswarm.portforward.error

import io.codeswarm.portforward.domain.{Endpoint, ForwardingStatus}

/**
 * Base exception for expected application-level forwarding failures.
 *
 * @param message human-readable description.
 * @param cause optional underlying cause.
 */
sealed abstract class PortForwardException(
    message: String,
    cause: Throwable = null
) extends RuntimeException(message, cause)

/**
 * Indicates that configuration could not be loaded or parsed.
 *
 * @param details description of the configuration problem.
 * @param underlying optional parser or I/O failure.
 */
final case class ConfigurationLoadException(
    details: String,
    underlying: Throwable = null
) extends PortForwardException(details, underlying)

/**
 * Indicates that forwarding configuration violates domain rules.
 *
 * @param errors all validation messages found in the configuration.
 */
final case class InvalidConfigurationException(errors: List[String])
    extends PortForwardException(errors.mkString("; "))

/**
 * Indicates that a lifecycle operation is illegal in the current state.
 *
 * @param operation requested lifecycle operation.
 * @param current current public forwarding state.
 */
final case class ForwarderStateException(
    operation: String,
    current: ForwardingStatus
) extends PortForwardException(
      s"Cannot $operation forwarder while state is $current"
    )

/**
 * Indicates that the local listener could not be bound.
 *
 * @param endpoint local endpoint that failed to bind.
 * @param underlying low-level bind failure.
 */
final case class BindFailedException(
    endpoint: Endpoint,
    underlying: Throwable
) extends PortForwardException(
      s"Unable to bind TCP listener to $endpoint: ${underlying.getMessage}",
      underlying
    )
