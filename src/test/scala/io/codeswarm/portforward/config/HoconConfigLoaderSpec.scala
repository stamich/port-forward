package io.codeswarm.portforward.config

import io.codeswarm.portforward.error.ConfigurationLoadException
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Tests the milestone 0.2 HOCON configuration adapter.
 */
final class HoconConfigLoaderSpec
    extends AnyFunSuite
    with Matchers {

  /** Ensures the bundled nested listen/target configuration is loadable. */
  test("load reads default classpath configuration") {
    val result =
      new HoconConfigLoader().load(None)

    result.isRight shouldBe true

    val config =
      result.toOption.get

    config.listen.host shouldBe "127.0.0.1"
    config.listen.port shouldBe 8090
    config.target.host shouldBe "example.com"
    config.target.port shouldBe 80
  }

  /** Ensures missing explicit files return a typed error. */
  test("load reports a missing explicit file") {
    val result =
      new HoconConfigLoader()
        .load(Some("definitely-missing.conf"))

    result.left.toOption.get shouldBe
      a[ConfigurationLoadException]

    result.left.toOption.get.getMessage should include(
      "does not exist"
    )
  }
}
