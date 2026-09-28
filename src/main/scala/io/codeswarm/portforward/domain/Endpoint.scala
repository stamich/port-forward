package io.codeswarm.portforward.domain

/**
 * Represents one TCP network endpoint.
 *
 * The class intentionally stores only host and port. DNS resolution belongs
 * to the networking runtime, not to the domain model.
 *
 * @param host DNS name or IP address.
 * @param port TCP port in the inclusive range 1..65535.
 */
final case class Endpoint(host: String, port: Int) {

  /**
   * Returns a human-readable endpoint representation.
   *
   * IPv6 literals are enclosed in square brackets to avoid ambiguity between
   * address separators and the port separator.
   *
   * @return endpoint rendered as `host:port` or `[ipv6]:port`.
   */
  override def toString: String =
    if (host.contains(":") && !host.startsWith("[")) s"[$host]:$port"
    else s"$host:$port"
}
