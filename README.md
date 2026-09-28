# Port Forward Server

A small TCP port-forwarding application written in Scala and based on Akka Streams.

Version **0.1.1** is a hardened/refactored milestone. Its purpose is to make the
existing single-rule TCP forwarder easier to understand, test and extend before
larger features are introduced.

## What the application does

The server accepts TCP connections on a local endpoint and forwards the byte
stream to a configured target endpoint:

```text
TCP client
    |
    v
local listen endpoint
    |
    v
Port Forward Server
    |
    v
remote target endpoint
```

Akka Streams is used to handle the connection streams and propagate
backpressure.

> Important: plain TCP port forwarding is **not encryption** and is not a VPN or
> SSH tunnel. Binding a listener to a public interface may expose the target
> service. TLS/mTLS support is planned for a later milestone.

## Milestone 0.1.1 goals

This milestone intentionally focuses on code quality instead of feature growth:

- SOLID/KISS/DRY/YAGNI-oriented package structure,
- separation of domain, configuration, networking, application service and UI,
- immutable domain objects,
- explicit configuration validation,
- structured logging instead of `println` in the networking layer,
- shared runtime lifecycle for CLI and GUI,
- idempotent forwarding shutdown,
- unit and TCP end-to-end tests,
- smaller dependency set,
- GitHub Actions CI,
- simplified non-root Docker runtime,
- updated documentation and changelog.

UDP, TLS/mTLS, multiple forwarding rules, metrics and management API are
deliberately outside the scope of 0.1.1.

## Architecture

```text
                         +----------------------+
                         | ForwardServerMainApp |
                         +----------+-----------+
                                    |
                     +--------------+--------------+
                     |                             |
                     v                             v
              +-------------+               +-------------+
              | CLI adapter |               | GUI adapter |
              +------+------+               +------+------+
                     |                             |
                     +--------------+--------------+
                                    |
                                    v
                          +-------------------+
                          | ForwardingService |
                          +---------+---------+
                                    |
                                    v
                              +-----------+
                              | Forwarder |
                              +-----+-----+
                                    |
                                    v
                            +---------------+
                            | TcpForwarder  |
                            +-------+-------+
                                    |
                                    v
                       +--------------------------+
                       | TcpConnectionFlowFactory |
                       +--------------------------+
```

### Package structure

```text
io.codeswarm.portforward
├── ForwardServerMainApplication.scala
├── cli
│   ├── CliCommand.scala
│   ├── CommandLineParser.scala
│   └── PortForwardCliApp.scala
├── config
│   ├── ConfigError.scala
│   ├── ConfigLoader.scala
│   └── HoconConfigLoader.scala
├── domain
│   ├── Endpoint.scala
│   ├── ForwardingConfig.scala
│   └── ForwardingStatus.scala
├── gui
│   └── PortForwardGuiApp.scala
├── network
│   ├── Forwarder.scala
│   └── tcp
│       ├── TcpConnectionFlowFactory.scala
│       └── TcpForwarder.scala
├── runtime
│   └── ApplicationRuntime.scala
├── service
│   └── ForwardingService.scala
└── validation
    └── ConfigValidator.scala
```

## Design principles applied

### Single Responsibility Principle

Each component has one primary reason to change:

- `Endpoint` / `ForwardingConfig` represent domain data.
- `ConfigValidator` validates domain rules.
- `HoconConfigLoader` translates HOCON to domain objects.
- `TcpConnectionFlowFactory` creates per-connection stream flows.
- `TcpForwarder` owns listener lifecycle.
- `ForwardingService` coordinates validation and transport.
- CLI and GUI contain presentation logic only.

### Dependency Inversion Principle

`ForwardingService` depends on the `Forwarder` trait rather than directly on
Akka TCP. This allows tests to use a lightweight stub and makes a future Pekko
migration easier.

### KISS and YAGNI

0.1.1 keeps one TCP forwarding rule. It does not add abstractions for UDP, TLS,
load balancing, multiple targets or observability until those features are
actually introduced.

## Requirements

For local development:

- JDK 17,
- SBT 1.10.7.

The code remains on Scala 2.13.16 and Akka 2.8.8 in this milestone. Migration
to Apache Pekko and a Scala/JDK modernization are planned for milestone 0.2.0.

## Build

```bash
sbt clean test
sbt assembly
```

The fat JAR is created as:

```text
target/scala-2.13/port-forward-server.jar
```

## Configuration

Default `application.conf`:

```hocon
port-forward {
  local-host = "127.0.0.1"
  local-port = 8090
  remote-host = "example.com"
  remote-port = 80
}
```

Valid TCP ports are `1..65535`. Host values must not be blank.

## CLI usage

Use the bundled configuration:

```bash
java -jar target/scala-2.13/port-forward-server.jar --cli
```

Use a custom HOCON file:

```bash
java -jar target/scala-2.13/port-forward-server.jar --cli ./application.conf
```

Pass endpoints directly:

```bash
java -jar target/scala-2.13/port-forward-server.jar \
  --cli 127.0.0.1 8090 example.com 80
```

Display help:

```bash
java -jar target/scala-2.13/port-forward-server.jar --cli --help
```

Stop an interactive CLI instance by typing:

```text
quit
```

`exit` and `stop` are also accepted.

## GUI usage

```bash
java -jar target/scala-2.13/port-forward-server.jar --gui
```

Launching the JAR with no mode selector also opens the GUI.

The GUI uses the same `ForwardingService` and networking implementation as the
CLI.

## Docker

Build the JAR first:

```bash
sbt assembly
```

Then build the image:

```bash
docker build -t port-forward:0.1.1 .
```

Run the CLI container:

```bash
docker run --rm \
  -p 8090:8090 \
  port-forward:0.1.1
```

The Docker image intentionally contains only the server/CLI runtime. GUI/X11
dependencies and Xvfb were removed from the container because a server
container should not emulate a desktop session.

## Tests

Run:

```bash
sbt test
```

The test suite covers:

- configuration validation,
- command-line parsing,
- default HOCON loading,
- application-service behavior via a stub transport,
- TCP end-to-end forwarding through a local echo server.

## Security notes

- Do not bind to `0.0.0.0` unless the forwarding listener is intentionally
  reachable from other hosts.
- 0.1.1 does not provide encryption, authentication, source allowlists or rate
  limiting.
- Use operating-system firewall rules when exposing a listener outside the
  local host.
- TLS/mTLS and access control belong to later milestones.

## Roadmap

### 0.1.1 — current

Code-quality hardening and architectural refactor.

### 0.2.0

Planned technology modernization:

- Akka -> Apache Pekko,
- dependency refresh,
- Scala 2.13 patch update,
- JDK baseline review.

### Later milestones

Potential features:

- multiple forwarding rules,
- UDP,
- IPv6 hardening,
- TLS/mTLS,
- timeouts and connection limits,
- Prometheus/OpenTelemetry observability,
- management API,
- configuration hot reload,
- target health checks and load balancing.

See [CHANGELOG.md](CHANGELOG.md).
