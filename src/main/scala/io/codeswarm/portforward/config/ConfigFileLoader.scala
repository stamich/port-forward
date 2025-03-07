package io.codeswarm.portforward.config

import com.typesafe.config.{Config, ConfigFactory}
import io.codeswarm.portforward.model.ForwardingConfig

import java.io.File
import scala.util.{Try, Success, Failure}

object ConfigFileLoader {
  private val DefaultConfigPath = "application.conf"

  def loadFromFile(configPath: String = DefaultConfigPath): Option[ForwardingConfig] = {
    Try {
      val config = if (new File(configPath).exists()) {
        ConfigFactory.parseFile(new File(configPath))
      } else {
        ConfigFactory.load(configPath)
      }

      createForwardingConfig(config)
    } match {
      case Success(config) => Some(config)
      case Failure(ex) =>
        println(s"Error loading configuration: ${ex.getMessage}")
        None
    }
  }

  private def createForwardingConfig(config: Config): ForwardingConfig = {
    val forwardConfig = config.getConfig("port-forward")
    ForwardingConfig(
      localHost = forwardConfig.getString("local-host"),
      localPort = forwardConfig.getInt("local-port"),
      remoteHost = forwardConfig.getString("remote-host"),
      remotePort = forwardConfig.getInt("remote-port"),
      bufferSize = if (forwardConfig.hasPath("buffer-size"))
        forwardConfig.getInt("buffer-size")
      else
        1024 * 1024
    )
  }

  def printFileConfigUsage(): Unit = {
    println("\nAlternatively, you can create a configuration file with the following format:")
    println("File: application.conf or specify path as first argument")
    println("""
              |port-forward {
              |  local-host = "localhost"
              |  local-port = 8080
              |  remote-host = "example.com"
              |  remote-port = 80
              |  buffer-size = 1048576  # Optional, defaults to 1MB
              |}
              |""".stripMargin)
  }
}
