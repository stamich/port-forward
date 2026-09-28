package io.codeswarm.portforward.config

import io.codeswarm.portforward.domain.ForwardingConfig
import io.codeswarm.portforward.error.PortForwardException

/**
 * Abstraction for loading one forwarding configuration.
 */
trait ConfigLoader {

  /**
   * Loads and validates forwarding configuration.
   *
   * @param path optional filesystem path. `None` selects classpath
   *             `application.conf`.
   * @return valid forwarding configuration or a typed application error.
   */
  def load(path: Option[String]): Either[PortForwardException, ForwardingConfig]
}
