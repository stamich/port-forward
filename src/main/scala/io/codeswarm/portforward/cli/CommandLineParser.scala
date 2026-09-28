package io.codeswarm.portforward.cli

import io.codeswarm.portforward.cli.CliCommand.{Help, Start, StartFromConfig}
import io.codeswarm.portforward.domain.{Endpoint, ForwardingConfig}

import scala.util.Try

/**
 * Pure parser converting raw command-line arguments into CLI commands.
 */
object CommandLineParser {

  /**
   * Parses arguments accepted after the optional `--cli` mode selector.
   *
   * Supported forms:
   *   - no arguments: load classpath `application.conf`
   *   - one argument: load the supplied configuration file
   *   - four arguments: `<local-host> <local-port> <remote-host> <remote-port>`
   *   - `--help` or `-h`: show usage
   *
   * @param args raw CLI arguments.
   * @return parsed command or explanatory error.
   */
  def parse(args: Array[String]): Either[String, CliCommand] =
    args.toList match {
      case Nil =>
        Right(StartFromConfig(None))

      case ("--help" | "-h") :: Nil =>
        Right(Help)

      case configPath :: Nil =>
        Right(StartFromConfig(Some(configPath)))

      case localHost :: localPort :: remoteHost :: remotePort :: Nil =>
        for {
          parsedLocalPort <- parsePort("local", localPort)
          parsedRemotePort <- parsePort("remote", remotePort)
        } yield Start(
          ForwardingConfig(
            listen = Endpoint(localHost, parsedLocalPort),
            target = Endpoint(remoteHost, parsedRemotePort)
          )
        )

      case _ =>
        Left("Invalid command-line arguments. Use --help to display supported forms.")
    }

  /**
   * Parses one TCP port.
   *
   * @param name logical port name used in error messages.
   * @param value textual numeric value.
   * @return parsed integer or explanatory error.
   */
  private def parsePort(name: String, value: String): Either[String, Int] =
    Try(value.toInt).toEither.left.map(_ => s"$name port must be a number: $value")

  /**
   * Returns the CLI usage text.
   *
   * @return multi-line help content.
   */
  def usage: String =
    """Port Forward Server 0.1.1
      |
      |Usage:
      |  java -jar port-forward-server.jar --cli
      |  java -jar port-forward-server.jar --cli <config-file>
      |  java -jar port-forward-server.jar --cli <local-host> <local-port> <remote-host> <remote-port>
      |  java -jar port-forward-server.jar --cli --help
      |
      |Examples:
      |  java -jar port-forward-server.jar --cli
      |  java -jar port-forward-server.jar --cli ./application.conf
      |  java -jar port-forward-server.jar --cli 127.0.0.1 8090 example.com 80
      |""".stripMargin
}
