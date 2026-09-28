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
 *
 * The default configuration is read explicitly from the application's
 * `application.conf` resource instead of using `ConfigFactory.load()`. This
 * keeps domain configuration loading independent from reference configuration
 * contributed by Pekko and other libraries on the classpath.
 */
final class HoconConfigLoader extends ConfigLoader {

  /** Classpath resource containing the bundled default configuration. */
  private val DefaultResource = "application.conf"

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
          loadFile(value)

        case None =>
          loadDefaultResource()
      }

      rawConfig.flatMap { config =>
        for {
          domain <- toDomain(config)
          valid <- validate(domain)
        } yield valid
      }
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
   * Loads an explicitly supplied configuration file.
   *
   * @param value path to the HOCON file.
   * @return parsed configuration or a typed loading error.
   */
  private def loadFile(
      value: String
  ): Either[PortForwardException, Config] = {
    val file = new File(value)

    if (!file.isFile)
      Left(
        ConfigurationLoadException(
          s"Configuration file does not exist: $value"
        )
      )
    else
      Right(ConfigFactory.parseFile(file).resolve())
  }

  /**
   * Loads the bundled application configuration directly from the classpath.
   *
   * Loading the application resource explicitly avoids merging unrelated
   * `reference.conf` files supplied by dependencies. Those files belong to
   * the runtime libraries and are not part of the port-forward domain schema.
   *
   * @return parsed default configuration or a typed loading error.
   */
  private def loadDefaultResource(): Either[PortForwardException, Config] = {
    val classLoader = getClass.getClassLoader
    val resource = classLoader.getResource(DefaultResource)

    if (resource == null)
      Left(
        ConfigurationLoadException(
          s"Classpath configuration resource not found: $DefaultResource"
        )
      )
    else
      Right(
        ConfigFactory
          .parseURL(resource)
          .resolve()
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
