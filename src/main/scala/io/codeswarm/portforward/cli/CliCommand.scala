package io.codeswarm.portforward.cli

import io.codeswarm.portforward.domain.ForwardingConfig

/**
 * Closed set of supported CLI commands.
 */
sealed trait CliCommand

/**
 * CLI command definitions.
 */
object CliCommand {

  /**
   * Starts forwarding with explicit endpoints.
   *
   * @param config forwarding rule.
   */
  final case class Start(config: ForwardingConfig) extends CliCommand

  /**
   * Starts forwarding from HOCON.
   *
   * @param configPath optional file path; `None` selects the bundled config.
   */
  final case class StartFromConfig(
      configPath: Option[String]
  ) extends CliCommand

  /**
   * Validates configuration without opening a TCP listener.
   *
   * @param configPath optional file path; `None` selects the bundled config.
   */
  final case class ValidateConfig(
      configPath: Option[String]
  ) extends CliCommand

  /** Displays command-line help. */
  case object Help extends CliCommand

  /** Displays application version. */
  case object Version extends CliCommand
}
