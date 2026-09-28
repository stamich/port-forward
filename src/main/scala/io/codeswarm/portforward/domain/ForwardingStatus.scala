package io.codeswarm.portforward.domain

/**
 * Describes the lifecycle state of the forwarding service.
 */
sealed trait ForwardingStatus

/**
 * Companion object containing all supported forwarding lifecycle states.
 */
object ForwardingStatus {

  /** The service has not been started or has already been stopped. */
  case object Stopped extends ForwardingStatus

  /** The service is starting and waiting for the TCP listener to bind. */
  case object Starting extends ForwardingStatus

  /** The TCP listener is bound and can accept client connections. */
  case object Running extends ForwardingStatus

  /** The service is unbinding the TCP listener. */
  case object Stopping extends ForwardingStatus
}
