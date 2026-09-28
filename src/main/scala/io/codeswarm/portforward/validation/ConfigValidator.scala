package io.codeswarm.portforward.validation

import io.codeswarm.portforward.domain.{Endpoint, ForwardingConfig}

/**
 * Stateless domain validator shared by all configuration entry points.
 */
object ConfigValidator {

  private val MinimumPort = 1
  private val MaximumPort = 65535

  /**
   * Validates one forwarding rule.
   *
   * DNS resolution is deliberately not performed here. A host name can be
   * syntactically valid even when DNS is temporarily unavailable.
   *
   * @param config configuration to validate.
   * @return `Right(config)` when valid, otherwise every discovered error.
   */
  def validate(
      config: ForwardingConfig
  ): Either[List[String], ForwardingConfig] = {
    val errors =
      validateEndpoint("listen", config.listen) ++
        validateEndpoint("target", config.target)

    Either.cond(errors.isEmpty, config, errors)
  }

  /**
   * Validates one endpoint.
   *
   * @param name logical endpoint name used in validation messages.
   * @param endpoint endpoint to validate.
   * @return zero or more validation messages.
   */
  private def validateEndpoint(
      name: String,
      endpoint: Endpoint
  ): List[String] = {
    val hostErrors =
      Option(endpoint.host)
        .filter(_.trim.nonEmpty)
        .fold(List(s"$name host must not be empty"))(_ => Nil)

    val portErrors =
      if (endpoint.port < MinimumPort || endpoint.port > MaximumPort)
        List(
          s"$name port must be between $MinimumPort and $MaximumPort"
        )
      else Nil

    hostErrors ++ portErrors
  }
}
