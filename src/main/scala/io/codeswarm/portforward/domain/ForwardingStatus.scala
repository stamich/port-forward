package io.codeswarm.portforward.domain

/**
 * Public lifecycle state of the forwarding service.
 */
sealed trait ForwardingStatus

/**
 * Supported forwarding lifecycle states.
 */
object ForwardingStatus {

  /** The listener is not active. */
  case object Stopped extends ForwardingStatus

  /** The listener is currently being bound. */
  case object Starting extends ForwardingStatus

  /** The listener is active and accepts client connections. */
  case object Running extends ForwardingStatus

  /** The listener is currently being unbound. */
  case object Stopping extends ForwardingStatus
}
