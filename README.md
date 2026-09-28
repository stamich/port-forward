# Port Forward Server

A small TCP port-forwarding application written in Scala and based on Apache
Pekko Streams.

Milestone **0.2.0** is the technology/runtime modernization release following
the architectural cleanup performed in 0.1.1.

## Technology baseline

- Scala 2.13.18
- JDK 21
- Apache Pekko 1.7.0
- Pekko Streams
- ScalaFX / JavaFX
- ScalaTest
- SLF4J + Logback
- SBT

## What changed in 0.2.0

- Akka 2.8 was replaced by Apache Pekko 1.7.
- Scala was upgraded from 2.13.16 to 2.13.18.
- JDK 21 is now the project baseline.
- forwarding lifecycle is represented by one coherent internal state object.
- Pekko `CoordinatedShutdown` owns graceful runtime cleanup.
- HOCON now mirrors the `listen` / `target` domain model.
- CLI can print version and validate configuration without opening sockets.
- typed application exceptions replace loosely structured config errors.
- TCP integration tests now cover lifecycle, bind failure and large payloads.
- scalafmt formatting checks are part of CI.
- assembly artifacts include their application version.

## Scope

0.2.0 intentionally remains:

```text
single forwarding rule
TCP only
CLI + GUI
HOCON config
```

UDP, TLS/mTLS, multiple rules, metrics, management API and load balancing are
future features.

## Architecture

```text
                         +----------------------+
                         | Main application     |
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
                                    ^
                                    |
                            +-------+-------+
                            | TcpForwarder  |
                            +-------+-------+
                                    |
                                    v
                       +--------------------------+
                       | Apache Pekko Streams     |
                       +--------------------------+
```

See `docs/ARCHITECTURE.md` for details.

## Why Apache Pekko

The project uses Apache Pekko as the reactive runtime while keeping the
`Forwarder` boundary independent of it. Pekko provides the ActorSystem and
Streams/TCP APIs needed by this project without changing the conceptual
forwarding architecture.

Migration details are documented in:

```text
docs/MIGRATION_AKKA_TO_PEKKO.md
```

## Configuration

0.2.0 uses the nested schema:

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

The old 0.1.x flat keys are intentionally not retained because the project is
still pre-1.0.

Validation rules:

- host must not be blank,
- port must be in `1..65535`,
- DNS is not resolved during validation.

## Build

Requirements:

- JDK 21
- SBT 1.12.9

Format:

```bash
sbt scalafmtAll scalafmtSbt
```

Verify formatting:

```bash
sbt scalafmtCheckAll scalafmtSbtCheck
```

Compile and test:

```bash
sbt clean test
```

Create the fat JAR:

```bash
sbt assembly
```

Artifact:

```text
target/scala-2.13/port-forward-server-0.2.0.jar
```

## CLI

Start using bundled `application.conf`:

```bash
java -jar target/scala-2.13/port-forward-server-0.2.0.jar --cli
```

Use an external config:

```bash
java -jar target/scala-2.13/port-forward-server-0.2.0.jar \
  --cli ./application.conf
```

Use direct endpoints:

```bash
java -jar target/scala-2.13/port-forward-server-0.2.0.jar \
  --cli 127.0.0.1 8090 example.com 80
```

Validate bundled config without binding a port:

```bash
java -jar target/scala-2.13/port-forward-server-0.2.0.jar \
  --cli --validate
```

Validate external config:

```bash
java -jar target/scala-2.13/port-forward-server-0.2.0.jar \
  --cli --validate ./application.conf
```

Version:

```bash
java -jar target/scala-2.13/port-forward-server-0.2.0.jar --version
```

Stop an interactive CLI session by typing:

```text
quit
```

`exit` and `stop` are also accepted.

## GUI

```bash
java -jar target/scala-2.13/port-forward-server-0.2.0.jar --gui
```

No mode flag also selects the GUI.

The GUI contains presentation logic only. It delegates forwarding to the same
`ForwardingService` used by CLI.

## Docker

Build the JAR first:

```bash
sbt assembly
```

Build the image:

```bash
docker build -t port-forward:0.2.0 .
```

Run:

```bash
docker run --rm \
  -p 8090:8090 \
  port-forward:0.2.0
```

The container runs CLI/server mode as a non-root user. It intentionally contains
no X11/Xvfb desktop emulation.

## Tests

Run:

```bash
sbt test
```

Coverage includes:

- endpoint rendering,
- configuration validation,
- nested HOCON loading,
- CLI parsing,
- service behavior via a stub transport,
- real TCP bidirectional forwarding,
- start/stop lifecycle,
- idempotent stop,
- duplicate start rejection,
- listener bind failure,
- recovery to `Stopped` after failed bind,
- large-payload byte integrity.

## Shutdown behavior

Pekko `CoordinatedShutdown` is used for runtime cleanup. The TCP listener is
unbound in `PhaseServiceUnbind`.

In milestone 0.2.0, stopping means:

```text
stop accepting new connections
```

Existing client streams may complete naturally. Explicit drain/force policies
are intentionally deferred.

## Security

Plain TCP forwarding does not provide encryption, authentication or VPN
semantics.

Do not expose the listener on `0.0.0.0` unless that is intentional and protected
by appropriate network/firewall rules.

0.2.0 does not yet provide:

- TLS/mTLS,
- source allowlists,
- rate limiting,
- connection limits.

## Design principles

The implementation follows:

- SOLID,
- KISS,
- DRY,
- YAGNI,
- immutable domain data,
- dependency inversion between service and transport,
- single responsibility between config/validation/network/runtime/UI.

No abstraction for a future feature is introduced until the project has an
actual use for it.

## Roadmap

### 0.2.0 — current

Pekko migration and runtime hardening.

### 0.2.1

Proposed next scope:

- connect timeout,
- idle timeout,
- connection limit,
- active connection tracking,
- stronger graceful shutdown/draining,
- additional failure-path tests.

### 0.3.0

Proposed:

- multiple forwarding rules,
- rule registry/lifecycle.

### 0.3.1+

Candidates:

- UDP,
- stronger IPv6 coverage,
- TLS/mTLS,
- management API,
- Prometheus/OpenTelemetry,
- hot reload,
- load balancing and backend health checks.

## Documentation

- `docs/ARCHITECTURE.md`
- `docs/MIGRATION_AKKA_TO_PEKKO.md`
- `docs/MILESTONE_0.2.0_TASKS.md`
- `CHANGELOG.md`
