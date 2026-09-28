package io.codeswarm.portforward.domain

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Tests endpoint rendering used by logs and CLI output.
 */
final class EndpointSpec
    extends AnyFunSuite
    with Matchers {

  /** Verifies normal host and IPv4 rendering. */
  test("toString renders regular host and port") {
    Endpoint("localhost", 8080).toString shouldBe
      "localhost:8080"
  }

  /** Verifies unambiguous IPv6 rendering. */
  test("toString wraps IPv6 literals in brackets") {
    Endpoint("::1", 8080).toString shouldBe
      "[::1]:8080"
  }
}
