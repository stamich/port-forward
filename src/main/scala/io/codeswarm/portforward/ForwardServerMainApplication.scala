package io.codeswarm.portforward

import io.codeswarm.portforward.cli.PortForwardCliApp
import io.codeswarm.portforward.gui.PortForwardGuiApp

/**
 * Main application entry point selecting either CLI or GUI presentation mode.
 */
object ForwardServerMainApplication {

  /**
   * Starts the application.
   *
   * `--cli` / `-c` selects the command-line application.
   * `--gui` / `-g` or no mode flag selects the ScalaFX application.
   *
   * @param args process arguments.
   */
  def main(args: Array[String]): Unit = {
    val cliMode = args.contains("--cli") || args.contains("-c")
    val guiMode = args.contains("--gui") || args.contains("-g")

    if (cliMode && guiMode) {
      Console.err.println("Choose only one mode: --cli or --gui.")
      sys.exit(2)
    }

    if (cliMode) {
      val cliArgs = args.filterNot(arg => arg == "--cli" || arg == "-c")
      PortForwardCliApp.main(cliArgs)
    } else {
      val unsupported = args.filterNot(arg => arg == "--gui" || arg == "-g")
      if (unsupported.nonEmpty) {
        Console.err.println("Arguments without --cli are not supported in GUI mode.")
        sys.exit(2)
      }

      PortForwardGuiApp.main(Array.empty)
    }
  }
}
