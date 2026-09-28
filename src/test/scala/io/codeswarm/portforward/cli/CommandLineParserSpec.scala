package io.codeswarm.portforward.cli

import io.codeswarm.portforward.cli.CliCommand.{Help, Start, StartFromConfig}
import io.codeswarm.portforward.domain.{Endpoint, ForwardingConfig}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Tests the pure command-line parser.
 */
final class CommandLineParserSpec extends AnyFunSuite with Matchers {

  /** Verifies the default configuration form. */
  test("parse uses classpath configuration for empty arguments") {
    CommandLineParser.parse(Array.empty) shouldBe Right(StartFromConfig(None))
  }

  /** Verifies explicit configuration file support. */
  test("parse accepts one configuration file path") {
    CommandLineParser.parse(Array("custom.conf")) shouldBe
      Right(StartFromConfig(Some("custom.conf")))
  }

  /** Verifies the direct four-argument forwarding form. */
  test("parse accepts direct endpoint arguments") {
    CommandLineParser.parse(Array("127.0.0.1", "9000", "localhost", "8080")) shouldBe
      Right(
        Start(
          ForwardingConfig(
            Endpoint("127.0.0.1", 9000),
            Endpoint("localhost", 8080)
          )
        )
      )
  }

  /** Verifies help parsing. */
  test("parse recognizes help") {
    CommandLineParser.parse(Array("--help")) shouldBe Right(Help)
  }

  /** Verifies malformed ports are rejected. */
  test("parse rejects non-numeric ports") {
    CommandLineParser.parse(Array("127.0.0.1", "bad", "localhost", "8080")).isLeft shouldBe true
  }
}
