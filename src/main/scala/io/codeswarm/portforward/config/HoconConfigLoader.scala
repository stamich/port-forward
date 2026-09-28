package io.codeswarm.portforward.config

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import io.codeswarm.portforward.domain.{Endpoint, ForwardingConfig}
import io.codeswarm.portforward.validation.ConfigValidator

import java.io.File
import scala.util.control.NonFatal

/**
 * Loads forwarding configuration from HOCON using Typesafe Config.
 */
final class HoconConfigLoader extends ConfigLoader {

  /**
   * Loads configuration from the supplied file or from the application
   * classpath when no explicit file was supplied.
   *
   * @param path optional path to a HOCON configuration file.
   * @return parsed and validated forwarding configuration.
   */
  override def load(path: Option[String]): Either[ConfigError, ForwardingConfig] = {
    try {
      val rawConfig = path match {
        case Some(value) =>
          val file = new File(value)
          if (!file.isFile)
            return Left(ConfigError(s"Configuration file does not exist: $value"))

          ConfigFactory.parseFile(file).resolve()

        case None =>
          ConfigFactory.load().resolve()
      }

      toDomain(rawConfig).flatMap(validate)
    } catch {
      case ex: ConfigException =>
        Left(ConfigError(s"Invalid configuration: ${ex.getMessage}", Some(ex)))
      case NonFatal(ex) =>
        Left(ConfigError(s"Unable to load configuration: ${ex.getMessage}", Some(ex)))
    }
  }

  /**
   * Converts raw HOCON data into the domain model.
   *
   * @param config raw Typesafe configuration.
   * @return domain forwarding configuration.
   */
  private def toDomain(config: Config): Either[ConfigError, ForwardingConfig] = {
    try {
      val forwarding = config.getConfig("port-forward")

      Right(
        ForwardingConfig(
          listen = Endpoint(
            host = forwarding.getString("local-host"),
            port = forwarding.getInt("local-port")
          ),
          target = Endpoint(
            host = forwarding.getString("remote-host"),
            port = forwarding.getInt("remote-port")
          )
        )
      )
    } catch {
      case ex: ConfigException =>
        Left(ConfigError(s"Missing or invalid port-forward configuration: ${ex.getMessage}", Some(ex)))
    }
  }

  /**
   * Applies domain-level validation after parsing.
   *
   * @param config parsed forwarding configuration.
   * @return valid configuration or a combined validation message.
   */
  private def validate(config: ForwardingConfig): Either[ConfigError, ForwardingConfig] =
    ConfigValidator
      .validate(config)
      .left
      .map(errors => ConfigError(errors.mkString("; ")))
}
