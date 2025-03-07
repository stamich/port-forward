package io.codeswarm.portforward.cli

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import io.codeswarm.portforward.config.ConfigFileLoader
import io.codeswarm.portforward.server.PortForwardServer

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.util.{Failure, Success}

object PortForwardCliApp {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Any] = ActorSystem(Behaviors.empty, "PortForwardSystem")
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    val config = CommandLineParser.parse(args) match {
      case Some(config) => Some(config)
      case None if args.isEmpty =>
        ConfigFileLoader.loadFromFile()
      case None if args.length == 1 =>
        ConfigFileLoader.loadFromFile(args(0))
      case None => None
    }

    config match {
      case Some(c) =>
        println(s"Starting port forwarding from ${c.localHost}:${c.localPort} to ${c.remoteHost}:${c.remotePort}")
        println("Type 'quit', 'exit', or 'stop' to terminate the server")

        val server = new PortForwardServer(c)
        server.start() onComplete {
          case Success(_) => println("Server terminated normally")
          case Failure(ex) =>
            println(s"Server terminated with error: ${ex.getMessage}")
            system.terminate()
        }
        Future {
          var running = true
          while (running) {
            val input = StdIn.readLine()
            input match {
              case "quit" | "exit" | "stop" =>
                println("Shutting down server...")
                server.stop()
                system.terminate()
                running = false
              case _ =>
            }
          }
        }

        sys.addShutdownHook {
          println("Shutting down...")
          server.stop()
          system.terminate()
        }

      case None =>
        println("Invalid arguments or configuration")
        system.terminate()
    }
  }
}
