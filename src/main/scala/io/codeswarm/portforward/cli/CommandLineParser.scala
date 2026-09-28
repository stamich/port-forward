package io.codeswarm.portforward.cli

import io.codeswarm.portforward.Version
import io.codeswarm.portforward.cli.CliCommand.{Help, Start, StartFromConfig, ValidateConfig}
import io.codeswarm.portforward.domain.{Endpoint, ForwardingConfig}

import scala.util.Try

/**
 * Pure parser converting raw command-line arguments into CLI commands.
 */
object CommandLineParser {

  /**
   * Parses arguments passed after the global `--cli` selector.
   *
   * @param args raw CLI arguments.
   * @return parsed command or a descriptive syntax error.
   */
  def parse(args: Array[String]): Either[String, CliCommand] =
    args.toList match {
      case Nil =>
        Right(StartFromConfig(None))

      case ("--help" | "-h") :: Nil =>
        Right(Help)

      case "--version" :: Nil =>
        Right(CliCommand.Version)

      case "--validate" :: Nil =>
        Right(ValidateConfig(None))

      case "--validate" :: configPath :: Nil =>
        Right(ValidateConfig(Some(configPath)))

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
        Left(
          "Invalid command-line arguments. Use --help to display supported forms."
        )
    }

  /**
   * Parses one numeric TCP port.
   *
   * @param name logical port name used in error messages.
   * @param value textual number.
   * @return integer value or parsing error.
   */
  private def parsePort(
      name: String,
      value: String
  ): Either[String, Int] =
    Try(value.toInt).toEither.left.map { _ =>
      s"$name port must be a number: $value"
    }

  /**
   * Returns CLI usage text.
   *
   * @return multi-line usage documentation.
   */
  def usage: String =
    s"""Port Forward Server ${Version.Current}
       |
       |Usage:
       |  java -jar port-forward-server-${Version.Current}.jar --cli
       |  java -jar port-forward-server-${Version.Current}.jar --cli <config-file>
       |  java -jar port-forward-server-${Version.Current}.jar --cli <local-host> <local-port> <remote-host> <remote-port>
       |  java -jar port-forward-server-${Version.Current}.jar --cli --validate [config-file]
       |  java -jar port-forward-server-${Version.Current}.jar --cli --version
       |  java -jar port-forward-server-${Version.Current}.jar --cli --help
       |""".stripMargin
}
