# Architecture — milestone 0.2.0

## Purpose

Milestone 0.2.0 migrates the networking runtime from Akka to Apache Pekko,
upgrades the Scala/JDK baseline and hardens lifecycle behavior while preserving
the intentionally small feature scope of one TCP rule.

## Dependency boundaries

```text
+-------------------------+
| CLI / ScalaFX GUI       |
+------------+------------+
             |
             v
+-------------------------+
| ForwardingService       |
+------------+------------+
             |
             v
+-------------------------+
| Forwarder abstraction   |
+------------+------------+
             ^
             |
+------------+------------+
| TcpForwarder            |
| TcpConnectionFlowFactory|
+------------+------------+
             |
             v
+-------------------------+
| Apache Pekko Streams    |
+-------------------------+
```

The domain, validation and application service layers do not import Pekko.

## Packages

```text
io.codeswarm.portforward
├── ForwardServerMainApplication.scala
├── Version.scala
├── cli/
├── config/
├── domain/
├── error/
├── gui/
├── network/
│   └── tcp/
├── runtime/
├── service/
└── validation/
```

## Lifecycle state model

`TcpForwarder` stores one internal state value:

```text
Idle
 |
 | start
 v
Binding(config)
 |
 | bind success
 v
Bound(config, ServerBinding)
 |
 | stop
 v
Unbinding(config)
 |
 | unbind success
 v
Idle
```

The public mapping is:

```text
Idle       -> Stopped
Binding    -> Starting
Bound      -> Running
Unbinding  -> Stopping
```

This is intentionally preferable to separate `status` and `binding` mutable
references because invalid combinations cannot be represented.

## Shutdown

Pekko `CoordinatedShutdown` owns process cleanup.

The runtime registers `ForwardingService.stop()` in
`PhaseServiceUnbind`. Explicit shutdown and JVM/process shutdown therefore use
the same cleanup path.

`stop()` unbinds the listener, preventing new connections. Existing connection
streams are allowed to finish naturally in 0.2.0. Drain/force semantics remain
future work.

## TCP stream

For every accepted client:

```text
client socket
    |
    v
IncomingConnection
    |
    v
TcpConnectionFlowFactory
    |
    v
Tcp().outgoingConnection(target)
    |
    v
target socket
```

Pekko Streams propagates backpressure. The application does not add an
unbounded byte buffer.

## Configuration boundary

The HOCON structure now mirrors the domain model:

```hocon
port-forward {
  listen {
    host = "127.0.0.1"
    port = 8090
  }

  target {
    host = "example.com"
    port = 80
  }
}
```

The old flat `local-host` / `remote-host` schema is intentionally removed
before 1.0.

## Error model

Expected application failures extend `PortForwardException`:

- `ConfigurationLoadException`
- `InvalidConfigurationException`
- `ForwarderStateException`
- `BindFailedException`

The asynchronous transport API remains `Future[Done]`; `Future[Either[...]]`
would add unnecessary nesting at the current scale.

## Design principles

### SOLID

- SRP: config, validation, service, runtime and TCP behavior are separated.
- OCP: a future transport can implement `Forwarder`.
- LSP: service callers use the same lifecycle contract for every implementation.
- ISP: `Forwarder` exposes only start/stop/status.
- DIP: `ForwardingService` depends on `Forwarder`, not `TcpForwarder`.

### KISS / YAGNI

0.2.0 does not add UDP, TLS, multiple forwarding rules, metrics, REST API,
circuit breakers or load balancing.

### DRY

CLI, GUI and HOCON share the same domain model, validator and service layer.

## Technology baseline

- Scala 2.13.18
- JDK 21
- Apache Pekko 1.7.0
- ScalaFX / JavaFX
- ScalaTest
- SLF4J + Logback

## Next architectural step

The next milestone should focus on runtime behavior such as timeouts,
connection limits and stronger graceful shutdown before introducing protocol
or management-plane expansion.
