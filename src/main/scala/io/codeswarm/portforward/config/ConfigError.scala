package io.codeswarm.portforward.config

/**
 * Represents a configuration loading or validation failure.
 *
 * @param message human-readable problem description.
 * @param cause optional underlying exception.
 */
final case class ConfigError(
    message: String,
    cause: Option[Throwable] = None
)
