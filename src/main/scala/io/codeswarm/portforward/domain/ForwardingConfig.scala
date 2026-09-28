package io.codeswarm.portforward.domain

/**
 * Immutable configuration of one TCP forwarding rule.
 *
 * Milestone 0.2.0 intentionally remains single-rule and TCP-only. Features
 * such as UDP, TLS and multiple rules are deferred to later milestones to keep
 * this release focused on the Pekko migration and runtime hardening.
 *
 * @param listen endpoint on which clients connect.
 * @param target endpoint receiving forwarded TCP traffic.
 */
final case class ForwardingConfig(
    listen: Endpoint,
    target: Endpoint
)
