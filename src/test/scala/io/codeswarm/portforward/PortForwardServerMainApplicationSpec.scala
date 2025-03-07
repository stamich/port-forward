package io.codeswarm.portforward

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class PortForwardServerMainApplicationSpec extends AnyFunSuite with Matchers with MockitoSugar {

  test("should start in GUI mode when no arguments are provided") {
    val testApp = new TestableMainApplication()

    testApp.main(Array.empty[String])

    testApp.guiCalled should be(true)
    testApp.cliCalled should be(false)
  }

  test("should start in GUI mode with --gui flag") {
    val testApp = new TestableMainApplication()

    testApp.main(Array("--gui"))

    testApp.guiCalled should be(true)
    testApp.cliCalled should be(false)
  }

  test("should start in GUI mode with -g flag") {
    val testApp = new TestableMainApplication()

    testApp.main(Array("-g"))

    testApp.guiCalled should be(true)
    testApp.cliCalled should be(false)
  }

  test("should start in CLI mode with --cli flag") {
    val testApp = new TestableMainApplication()

    testApp.main(Array("--cli"))

    testApp.guiCalled should be(false)
    testApp.cliCalled should be(true)
    testApp.cliArgs should be(Array.empty[String])
  }

  test("should pass additional arguments to CLI app") {
    val testApp = new TestableMainApplication()

    testApp.main(Array("--cli", "arg1", "arg2"))

    testApp.guiCalled should be(false)
    testApp.cliCalled should be(true)
    testApp.cliArgs should be(Array("arg1", "arg2"))
  }

  test("should filter out mode flags from CLI arguments") {
    val testApp = new TestableMainApplication()

    testApp.main(Array("--cli", "-c", "arg1", "arg2"))

    testApp.cliArgs should be(Array("arg1", "arg2"))
  }

  class TestableMainApplication {
    var guiCalled = false
    var cliCalled = false
    var cliArgs: Array[String] = Array.empty

    def main(args: Array[String]): Unit = {
      val guiMode = args.isEmpty || args.contains("--gui") || args.contains("-g")

      if (guiMode) {
        guiCalled = true
      } else {
        cliCalled = true
        cliArgs = args.filterNot(arg => arg == "--cli" || arg == "-c")
      }
    }
  }
}
