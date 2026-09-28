package io.codeswarm.portforward.domain

/**
 * Represents one network endpoint used by the forwarding engine.
 *
 * @param host DNS name or IP address.
 * @param port TCP port in the inclusive range 1..65535.
 */
final case class Endpoint(host: String, port: Int) {

  /**
   * Returns a human-readable representation of this endpoint.
   *
   * @return endpoint rendered as `host:port`.
   */
  override def toString: String = s"$host:$port"
}
