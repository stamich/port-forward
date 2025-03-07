package io.codeswarm.portforward

import io.codeswarm.portforward.cli.PortForwardCliApp
import io.codeswarm.portforward.gui.PortForwardGuiApp

object ForwardServerMainApplication {

  def main(args: Array[String]): Unit = {
    val guiMode = args.isEmpty || args.contains("--gui") || args.contains("-g")

    if (guiMode) {
      println("Starting GUI mode...")
      PortForwardGuiApp.main(Array.empty)
    } else {
      println("Starting CLI mode...")
      val cliArgs = args.filterNot(arg => arg == "--cli" || arg == "-c")
      PortForwardCliApp.main(cliArgs)
    }
  }
}
