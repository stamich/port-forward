package io.codeswarm.portforward.cli

import io.codeswarm.portforward.domain.ForwardingConfig

/**
 * Algebra of commands accepted by the CLI parser.
 */
sealed trait CliCommand

/**
 * Companion object containing all CLI commands.
 */
object CliCommand {

  /**
   * Starts forwarding with an explicitly supplied configuration.
   *
   * @param config forwarding rule.
   */
  final case class Start(config: ForwardingConfig) extends CliCommand

  /**
   * Starts forwarding from a HOCON configuration source.
   *
   * @param configPath optional configuration path; `None` means classpath
   *                   `application.conf`.
   */
  final case class StartFromConfig(configPath: Option[String]) extends CliCommand

  /** Requests command-line help. */
  case object Help extends CliCommand
}
