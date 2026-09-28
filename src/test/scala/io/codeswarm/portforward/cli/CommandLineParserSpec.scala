package io.codeswarm.portforward.cli

import io.codeswarm.portforward.cli.CliCommand._
import io.codeswarm.portforward.domain.{Endpoint, ForwardingConfig}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Tests the side-effect-free CLI parser.
 */
final class CommandLineParserSpec
    extends AnyFunSuite
    with Matchers {

  /** Verifies default classpath configuration behavior. */
  test("parse uses classpath config for empty arguments") {
    CommandLineParser.parse(Array.empty) shouldBe
      Right(StartFromConfig(None))
  }

  /** Verifies explicit configuration file support. */
  test("parse accepts one config file path") {
    CommandLineParser.parse(Array("custom.conf")) shouldBe
      Right(StartFromConfig(Some("custom.conf")))
  }

  /** Verifies direct endpoint arguments. */
  test("parse accepts explicit endpoints") {
    CommandLineParser.parse(
      Array(
        "127.0.0.1",
        "9000",
        "localhost",
        "8080"
      )
    ) shouldBe
      Right(
        Start(
          ForwardingConfig(
            Endpoint("127.0.0.1", 9000),
            Endpoint("localhost", 8080)
          )
        )
      )
  }

  /** Verifies help command parsing. */
  test("parse recognizes help") {
    CommandLineParser.parse(Array("--help")) shouldBe
      Right(Help)
  }

  /** Verifies version command parsing. */
  test("parse recognizes version") {
    CommandLineParser.parse(Array("--version")) shouldBe
      Right(Version)
  }

  /** Verifies bundled config validation command. */
  test("parse recognizes default config validation") {
    CommandLineParser.parse(Array("--validate")) shouldBe
      Right(ValidateConfig(None))
  }

  /** Verifies explicit config validation command. */
  test("parse recognizes explicit config validation") {
    CommandLineParser.parse(
      Array("--validate", "custom.conf")
    ) shouldBe
      Right(ValidateConfig(Some("custom.conf")))
  }

  /** Verifies malformed numeric ports are rejected. */
  test("parse rejects non-numeric ports") {
    CommandLineParser
      .parse(
        Array(
          "127.0.0.1",
          "bad",
          "localhost",
          "8080"
        )
      )
      .isLeft shouldBe true
  }
}
