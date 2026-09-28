package io.codeswarm.portforward

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Protects the explicit public milestone version constant.
 */
final class VersionSpec
    extends AnyFunSuite
    with Matchers {

  /** Ensures code and release metadata remain aligned. */
  test("version is 0.2.0") {
    Version.Current shouldBe "0.2.0"
  }
}
