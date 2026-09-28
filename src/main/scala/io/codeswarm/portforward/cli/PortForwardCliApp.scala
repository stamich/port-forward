package io.codeswarm.portforward.cli

import io.codeswarm.portforward.Version
import io.codeswarm.portforward.cli.CliCommand.{Help, Start, StartFromConfig, ValidateConfig}
import io.codeswarm.portforward.config.HoconConfigLoader
import io.codeswarm.portforward.domain.ForwardingConfig
import io.codeswarm.portforward.runtime.ApplicationRuntime

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.util.{Failure, Success}

/**
 * Command-line adapter for the forwarding application.
 */
object PortForwardCliApp {

  /**
   * Runs one CLI command.
   *
   * @param args arguments excluding the global `--cli` selector.
   */
  def main(args: Array[String]): Unit = {
    val loader = new HoconConfigLoader

    CommandLineParser.parse(args) match {
      case Right(Help) =>
        println(CommandLineParser.usage)

      case Right(CliCommand.Version) =>
        println(s"port-forward ${Version.Current}")

      case Right(Start(config)) =>
        run(config)

      case Right(StartFromConfig(path)) =>
        loader.load(path) match {
          case Right(config) =>
            run(config)

          case Left(error) =>
            Console.err.println(error.getMessage)
            sys.exit(2)
        }

      case Right(ValidateConfig(path)) =>
        loader.load(path) match {
          case Right(config) =>
            println("Configuration valid.")
            println(s"listen: ${config.listen}")
            println(s"target: ${config.target}")

          case Left(error) =>
            Console.err.println(
              s"Configuration invalid: ${error.getMessage}"
            )
            sys.exit(2)
        }

      case Left(error) =>
        Console.err.println(error)
        Console.err.println(CommandLineParser.usage)
        sys.exit(2)
    }
  }

  /**
   * Starts forwarding and keeps the process active until the user requests a
   * graceful shutdown or startup fails.
   *
   * @param config forwarding configuration.
   */
  private def run(config: ForwardingConfig): Unit = {
    val runtime = ApplicationRuntime.create()
    implicit val executionContext: ExecutionContextExecutor =
      runtime.actorSystem.dispatcher

    val started =
      runtime.forwardingService.start(config)

    started.onComplete {
      case Success(_) =>
        println(
          s"Forwarding ${config.listen} -> ${config.target}"
        )
        println(
          "Type 'quit', 'exit' or 'stop' and press Enter to terminate."
        )

      case Failure(ex) =>
        Console.err.println(
          s"Unable to start forwarding: ${ex.getMessage}"
        )
        runtime.shutdown()
    }

    val interactiveShutdown =
      started.flatMap(_ => waitForStopCommand())
        .flatMap(_ => runtime.shutdown())

    interactiveShutdown.failed.foreach { ex =>
      Console.err.println(
        s"Shutdown failed: ${ex.getMessage}"
      )
    }

    Await.ready(
      runtime.actorSystem.whenTerminated,
      Duration.Inf
    )
  }

  /**
   * Reads standard input until a supported stop command or EOF is received.
   *
   * @param executionContext execution context used for blocking input future.
   * @return future completed when shutdown was requested.
   */
  private def waitForStopCommand()(
      implicit executionContext: scala.concurrent.ExecutionContext
  ): Future[Unit] =
    Future {
      var finished = false

      while (!finished) {
        Option(StdIn.readLine()) match {
          case None =>
            finished = true

          case Some(value)
              if Set("quit", "exit", "stop")
                .contains(value.trim.toLowerCase) =>
            finished = true

          case _ =>
            ()
        }
      }
    }
}
