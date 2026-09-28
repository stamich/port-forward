package io.codeswarm.portforward.config

import io.codeswarm.portforward.error.ConfigurationLoadException
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.nio.charset.StandardCharsets
import java.nio.file.Files

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

    val config = result.fold(
      error => fail(s"Expected default configuration to load, but got: ${error.getMessage}", error),
      identity
    )

    config.listen.host shouldBe "127.0.0.1"
    config.listen.port shouldBe 8090
    config.target.host shouldBe "example.com"
    config.target.port shouldBe 80
  }

  /** Ensures an explicitly supplied valid HOCON file can be loaded. */
  test("load reads an explicit configuration file") {
    val file = Files.createTempFile("port-forward-", ".conf")

    try {
      Files.writeString(
        file,
        """
          |port-forward {
          |  listen { host = "127.0.0.1", port = 19090 }
          |  target { host = "localhost", port = 19091 }
          |}
          |""".stripMargin,
        StandardCharsets.UTF_8
      )

      val result =
        new HoconConfigLoader().load(Some(file.toString))

      val config = result.fold(
        error => fail(s"Expected explicit configuration to load, but got: ${error.getMessage}", error),
        identity
      )

      config.listen.port shouldBe 19090
      config.target.port shouldBe 19091
    } finally {
      Files.deleteIfExists(file)
    }
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
