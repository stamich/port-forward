package io.codeswarm.portforward.cli

import io.codeswarm.portforward.config.ConfigFileLoader
import io.codeswarm.portforward.model.ForwardingConfig

object CommandLineParser {
  def parse(args: Array[String]): Option[ForwardingConfig] = {
    if (args.isEmpty) {
      return None
    } else if (args.length == 1) {
      return None
    } else if (args.length < 4) {
      printUsage()
      ConfigFileLoader.printFileConfigUsage()
      return None
    }

    try {
      Some(ForwardingConfig(
        localHost = args(0),
        localPort = args(1).toInt,
        remoteHost = args(2),
        remotePort = args(3).toInt
      ))
    } catch {
      case e: NumberFormatException =>
        println("Error: Port must be a number")
        printUsage()
        ConfigFileLoader.printFileConfigUsage()
        None
    }
  }

  private def printUsage(): Unit = {
    println("Usage: scala-port-forward <local-host> <local-port> <remote-host> <remote-port>")
    println("Example: scala-port-forward localhost 8080 example.com 80")
  }
}
