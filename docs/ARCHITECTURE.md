# Architecture — milestone 0.1.1

## Purpose

Milestone 0.1.1 is a structural hardening release. The forwarding behavior
remains deliberately small: one TCP listener forwards byte streams to one TCP
target.

## Layers

### Domain

`Endpoint`, `ForwardingConfig` and `ForwardingStatus` contain no Akka, JavaFX or
Typesafe Config dependencies.

### Configuration

`ConfigLoader` defines the boundary. `HoconConfigLoader` is the concrete
Typesafe Config adapter.

### Validation

`ConfigValidator` owns domain validation. The same rules are therefore applied
regardless of whether configuration came from CLI, HOCON or GUI.

### Application service

`ForwardingService` coordinates validation and the forwarding transport.

### Network

`Forwarder` is the transport contract.

`TcpForwarder` owns TCP listener lifecycle.

`TcpConnectionFlowFactory` creates one Akka stream for each accepted client
connection and connects it to the target with `Tcp().outgoingConnection`.

### Runtime

`ApplicationRuntime` creates the ActorSystem, materializer, TCP implementation
and application service. It also centralizes shutdown.

### Presentation

`PortForwardCliApp` and `PortForwardGuiApp` are adapters. Neither implements
TCP forwarding.

## Dependency direction

```text
presentation
    |
    v
application service
    |
    v
Forwarder abstraction
    ^
    |
TCP implementation

configuration --> domain <-- validation
```

The domain layer is intentionally dependency-light.

## SOLID mapping

- **S**: networking, configuration, validation and presentation are separated.
- **O**: future transports can implement `Forwarder`.
- **L**: callers use the `Forwarder` lifecycle contract independent of the
  implementation.
- **I**: the forwarding interface contains only start/stop/status behavior.
- **D**: `ForwardingService` depends on `Forwarder`, not `TcpForwarder`.

## KISS / DRY / YAGNI

The milestone does not introduce multi-rule registries, protocol hierarchies,
TLS abstractions or observability interfaces that would not yet be used.

Shared validation and runtime construction remove duplication that previously
existed between CLI and GUI paths.

## Lifecycle

```text
Stopped
  |
  | start
  v
Starting
  |
  | bind success
  v
Running
  |
  | stop
  v
Stopping
  |
  | unbind complete
  v
Stopped
```

A failed bind returns the state to `Stopped`.

## Backpressure

No application-level unbounded byte buffer is introduced. Akka Streams
propagates demand between the accepted socket and the outgoing target
connection.

## Deferred concerns

The following are explicitly deferred:

- UDP,
- TLS/mTLS,
- multiple rules,
- source ACL,
- connection limits,
- observability metrics,
- management HTTP API,
- hot reload,
- load balancing.

Deferring these keeps 0.1.1 focused and makes later changes easier to review.
