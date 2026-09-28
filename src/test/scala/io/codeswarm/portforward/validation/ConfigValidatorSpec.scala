package io.codeswarm.portforward.validation

import io.codeswarm.portforward.domain.{Endpoint, ForwardingConfig}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Tests domain configuration validation.
 */
final class ConfigValidatorSpec extends AnyFunSuite with Matchers {

  /** Ensures a regular forwarding rule is accepted. */
  test("validate accepts valid endpoints") {
    val config = ForwardingConfig(
      Endpoint("127.0.0.1", 8090),
      Endpoint("example.com", 80)
    )

    ConfigValidator.validate(config) shouldBe Right(config)
  }

  /** Ensures invalid port values are rejected before networking starts. */
  test("validate rejects ports outside TCP range") {
    val config = ForwardingConfig(
      Endpoint("127.0.0.1", 0),
      Endpoint("example.com", 70000)
    )

    val result = ConfigValidator.validate(config)

    result.isLeft shouldBe true
    result.left.toOption.get should contain allOf (
      "listen port must be between 1 and 65535",
      "target port must be between 1 and 65535"
    )
  }

  /** Ensures blank host names are rejected. */
  test("validate rejects blank hosts") {
    val config = ForwardingConfig(
      Endpoint(" ", 8090),
      Endpoint("", 80)
    )

    ConfigValidator.validate(config).isLeft shouldBe true
  }
}
