package io.codeswarm.portforward.validation

import io.codeswarm.portforward.domain.{Endpoint, ForwardingConfig}

/**
 * Validates user-provided forwarding configuration before networking resources
 * are allocated.
 */
object ConfigValidator {

  private val MinimumPort = 1
  private val MaximumPort = 65535

  /**
   * Validates a complete forwarding configuration.
   *
   * @param config configuration to validate.
   * @return `Right(config)` when valid, otherwise `Left` containing all
   *         validation messages.
   */
  def validate(config: ForwardingConfig): Either[List[String], ForwardingConfig] = {
    val errors =
      validateEndpoint("listen", config.listen) ++
        validateEndpoint("target", config.target)

    if (errors.isEmpty) Right(config) else Left(errors)
  }

  /**
   * Validates one endpoint.
   *
   * @param name logical endpoint name used in validation messages.
   * @param endpoint endpoint to validate.
   * @return zero or more validation errors.
   */
  private def validateEndpoint(name: String, endpoint: Endpoint): List[String] = {
    val hostErrors =
      if (endpoint.host.trim.isEmpty) List(s"$name host must not be empty")
      else Nil

    val portErrors =
      if (endpoint.port < MinimumPort || endpoint.port > MaximumPort)
        List(s"$name port must be between $MinimumPort and $MaximumPort")
      else Nil

    hostErrors ++ portErrors
  }
}
