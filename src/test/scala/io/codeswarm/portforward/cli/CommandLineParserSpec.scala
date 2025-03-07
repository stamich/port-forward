package io.codeswarm.portforward.cli

import io.codeswarm.portforward.model.ForwardingConfig
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CommandLineParserSpec extends AnyFlatSpec with Matchers {

  "CommandLineParser" should "parse valid arguments correctly" in {
    val args = Array("localhost", "8080", "example.com", "80")
    val config = CommandLineParser.parse(args)

    config shouldBe defined
    config.get shouldEqual ForwardingConfig(
      localHost = "localhost",
      localPort = 8080,
      remoteHost = "example.com",
      remotePort = 80
    )
  }

  it should "return None for insufficient arguments" in {
    val args = Array("localhost", "8080", "example.com")
    val config = CommandLineParser.parse(args)

    config shouldBe None
  }

  it should "return None for invalid port number" in {
    val args = Array("localhost", "invalid", "example.com", "80")
    val config = CommandLineParser.parse(args)

    config shouldBe None
  }

  it should "return None for empty arguments" in {
    val config = CommandLineParser.parse(Array.empty[String])

    config shouldBe None
  }

  it should "return None for single argument" in {
    val config = CommandLineParser.parse(Array("config.conf"))

    config shouldBe None
  }
}
