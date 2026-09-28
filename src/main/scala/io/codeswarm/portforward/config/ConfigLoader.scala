package io.codeswarm.portforward.config

import io.codeswarm.portforward.domain.ForwardingConfig

/**
 * Abstraction for loading forwarding configuration.
 *
 * Keeping the loader behind a trait prevents the CLI and GUI from depending
 * directly on Typesafe Config and makes configuration behavior easy to test.
 */
trait ConfigLoader {

  /**
   * Loads one forwarding rule.
   *
   * @param path optional filesystem path. When absent, the application
   *             classpath `application.conf` is used.
   * @return either a descriptive configuration error or valid configuration.
   */
  def load(path: Option[String]): Either[ConfigError, ForwardingConfig]
}
