package io.codeswarm.portforward.cli

import io.codeswarm.portforward.cli.CliCommand.{Help, Start, StartFromConfig}
import io.codeswarm.portforward.config.HoconConfigLoader
import io.codeswarm.portforward.domain.ForwardingConfig
import io.codeswarm.portforward.runtime.ApplicationRuntime

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.util.{Failure, Success}

/**
 * Command-line front end for the port-forwarding application.
 */
object PortForwardCliApp {

  /**
   * Runs the CLI application.
   *
   * @param args command-line arguments excluding the global `--cli` selector.
   */
  def main(args: Array[String]): Unit = {
    val loader = new HoconConfigLoader

    CommandLineParser.parse(args) match {
      case Right(Help) =>
        println(CommandLineParser.usage)

      case Right(Start(config)) =>
        run(config)

      case Right(StartFromConfig(path)) =>
        loader.load(path) match {
          case Right(config) => run(config)
          case Left(error) =>
            Console.err.println(error.message)
            sys.exit(2)
        }

      case Left(error) =>
        Console.err.println(error)
        Console.err.println(CommandLineParser.usage)
        sys.exit(2)
    }
  }

  /**
   * Starts the forwarding runtime and keeps the process alive until the user
   * requests shutdown or the server fails.
   *
   * @param config forwarding rule to run.
   */
  private def run(config: ForwardingConfig): Unit = {
    val runtime = ApplicationRuntime.create()
    implicit val executionContext: ExecutionContextExecutor = runtime.actorSystem.dispatcher

    sys.addShutdownHook {
      Await.ready(runtime.shutdown(), Duration.Inf)
    }

    runtime.forwardingService.start(config).onComplete {
      case Success(_) =>
        println(s"Forwarding ${config.listen} -> ${config.target}")
        println("Type 'quit', 'exit' or 'stop' and press Enter to terminate.")

      case Failure(ex) =>
        Console.err.println(s"Unable to start forwarding: ${ex.getMessage}")
        runtime.shutdown()
    }

    waitForStopCommand().flatMap(_ => runtime.shutdown())
    Await.ready(runtime.actorSystem.whenTerminated, Duration.Inf)
  }

  /**
   * Reads standard input until a supported stop command is entered.
   *
   * @return future completed after a stop command or end-of-file.
   */
  private def waitForStopCommand()(implicit
      executionContext: scala.concurrent.ExecutionContext
  ): Future[Unit] =
    Future {
      var finished = false

      while (!finished) {
        Option(StdIn.readLine()) match {
          case None =>
            finished = true

          case Some(value) if Set("quit", "exit", "stop").contains(value.trim.toLowerCase) =>
            finished = true

          case _ =>
            ()
        }
      }
    }
}
