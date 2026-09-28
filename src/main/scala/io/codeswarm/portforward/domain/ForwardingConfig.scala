package io.codeswarm.portforward.domain

/**
 * Immutable configuration of one TCP forwarding rule.
 *
 * Milestone 0.1.1 deliberately keeps the domain model limited to a single
 * TCP rule. Multiple rules, UDP and TLS are intentionally deferred to later
 * milestones to keep this refactor focused and easy to reason about.
 *
 * @param listen endpoint on which the application accepts client connections.
 * @param target endpoint to which accepted TCP traffic is forwarded.
 */
final case class ForwardingConfig(
    listen: Endpoint,
    target: Endpoint
)
