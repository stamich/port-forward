package io.codeswarm.portforward.config

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import io.codeswarm.portforward.domain.{Endpoint, ForwardingConfig}
import io.codeswarm.portforward.error.{ConfigurationLoadException, InvalidConfigurationException, PortForwardException}
import io.codeswarm.portforward.validation.ConfigValidator

import java.io.File
import scala.util.control.NonFatal

/**
 * Typesafe Config adapter loading the milestone 0.2 HOCON schema.
 *
 * The expected shape is:
 *
 * {{{
 * port-forward {
 *   listen { host = "127.0.0.1", port = 8090 }
 *   target { host = "example.com", port = 80 }
 * }
 * }}}
 */
final class HoconConfigLoader extends ConfigLoader {

  /**
   * Loads and validates forwarding configuration.
   *
   * @param path optional external HOCON file path.
   * @return parsed configuration or a typed failure.
   */
  override def load(
      path: Option[String]
  ): Either[PortForwardException, ForwardingConfig] =
    try {
      val rawConfig = path match {
        case Some(value) =>
          val file = new File(value)
          if (!file.isFile)
            return Left(
              ConfigurationLoadException(
                s"Configuration file does not exist: $value"
              )
            )

          ConfigFactory.parseFile(file).resolve()

        case None =>
          ConfigFactory.load().resolve()
      }

      for {
        config <- toDomain(rawConfig)
        valid <- validate(config)
      } yield valid
    } catch {
      case ex: ConfigException =>
        Left(
          ConfigurationLoadException(
            s"Invalid configuration: ${ex.getMessage}",
            ex
          )
        )
      case NonFatal(ex) =>
        Left(
          ConfigurationLoadException(
            s"Unable to load configuration: ${ex.getMessage}",
            ex
          )
        )
    }

  /**
   * Converts the raw HOCON representation into immutable domain objects.
   *
   * @param config parsed Typesafe Config tree.
   * @return domain forwarding configuration or a parsing failure.
   */
  private def toDomain(
      config: Config
  ): Either[PortForwardException, ForwardingConfig] =
    try {
      val forwarding = config.getConfig("port-forward")
      val listen = forwarding.getConfig("listen")
      val target = forwarding.getConfig("target")

      Right(
        ForwardingConfig(
          listen = Endpoint(
            host = listen.getString("host"),
            port = listen.getInt("port")
          ),
          target = Endpoint(
            host = target.getString("host"),
            port = target.getInt("port")
          )
        )
      )
    } catch {
      case ex: ConfigException =>
        Left(
          ConfigurationLoadException(
            s"Missing or invalid port-forward configuration: ${ex.getMessage}",
            ex
          )
        )
    }

  /**
   * Applies shared domain validation after parsing.
   *
   * @param config parsed configuration.
   * @return validated configuration or a typed validation failure.
   */
  private def validate(
      config: ForwardingConfig
  ): Either[PortForwardException, ForwardingConfig] =
    ConfigValidator
      .validate(config)
      .left
      .map(InvalidConfigurationException.apply)
}
