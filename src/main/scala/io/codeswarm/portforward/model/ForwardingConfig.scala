package io.codeswarm.portforward.model

case class ForwardingConfig(
  localHost: String,
  localPort: Int,
  remoteHost: String,
  remotePort: Int,
  bufferSize: Int = 1 * 1024 * 1024
)
