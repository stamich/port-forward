package io.codeswarm.portforward

/**
 * Build version exposed by the command-line interface.
 *
 * A generated build-info module would be unnecessary complexity for the
 * current project size, so milestone 0.2.0 intentionally keeps one explicit
 * version constant.
 */
object Version {

  /** Current application version. */
  val Current: String = "0.2.0"
}
