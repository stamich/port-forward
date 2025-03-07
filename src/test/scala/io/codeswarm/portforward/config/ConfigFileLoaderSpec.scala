package io.codeswarm.portforward.config

import io.codeswarm.portforward.model.ForwardingConfig
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.io.{File, PrintWriter}

class ConfigFileLoaderSpec extends AnyFlatSpec with Matchers {

  "ConfigFileLoader" should "load valid configuration from file" in {
    val configFile = File.createTempFile("test-config", ".conf")
    configFile.deleteOnExit()

    val writer = new PrintWriter(configFile)
    writer.write("""
                   |port-forward {
                   |  local-host = "test-host"
                   |  local-port = 9090
                   |  remote-host = "test-remote"
                   |  remote-port = 8080
                   |  buffer-size = 2048
                   |}
    """.stripMargin)
    writer.close()

    val config = ConfigFileLoader.loadFromFile(configFile.getAbsolutePath)

    config shouldBe defined
    config.get shouldEqual ForwardingConfig(
      localHost = "test-host",
      localPort = 9090,
      remoteHost = "test-remote",
      remotePort = 8080,
      bufferSize = 2048
    )
  }

  it should "return None for non-existent file" in {
    val config = ConfigFileLoader.loadFromFile("non-existent-file.conf")

    config shouldBe None
  }

  it should "return None for malformed configuration" in {
    val configFile = File.createTempFile("malformed-config", ".conf")
    configFile.deleteOnExit()

    val writer = new PrintWriter(configFile)
    writer.write("""
                   |port-forward {
                   |  local-host = "test-host"
                   |  # Missing required fields
                   |}
    """.stripMargin)
    writer.close()

    val config = ConfigFileLoader.loadFromFile(configFile.getAbsolutePath)

    config shouldBe None
  }

  it should "use default buffer size when not specified" in {
    val configFile = File.createTempFile("default-buffer-config", ".conf")
    configFile.deleteOnExit()

    val writer = new PrintWriter(configFile)
    writer.write("""
                   |port-forward {
                   |  local-host = "test-host"
                   |  local-port = 9090
                   |  remote-host = "test-remote"
                   |  remote-port = 8080
                   |}
    """.stripMargin)
    writer.close()

    val config = ConfigFileLoader.loadFromFile(configFile.getAbsolutePath)

    config shouldBe defined
    config.get.bufferSize shouldEqual 1024 * 1024
  }
}
