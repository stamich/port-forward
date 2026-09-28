package io.codeswarm.portforward.config

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Tests configuration loading from the bundled application.conf.
 */
final class HoconConfigLoaderSpec extends AnyFunSuite with Matchers {

  /** Ensures the default classpath configuration remains loadable. */
  test("load reads default classpath configuration") {
    val loader = new HoconConfigLoader

    val result = loader.load(None)

    result.isRight shouldBe true
    val config = result.toOption.get
    config.listen.host shouldBe "127.0.0.1"
    config.listen.port shouldBe 8090
    config.target.host shouldBe "example.com"
    config.target.port shouldBe 80
  }

  /** Ensures missing explicit files return a descriptive error. */
  test("load reports a missing explicit file") {
    val loader = new HoconConfigLoader

    val result = loader.load(Some("definitely-missing.conf"))

    result.isLeft shouldBe true
    result.left.toOption.get.message should include("does not exist")
  }
}
