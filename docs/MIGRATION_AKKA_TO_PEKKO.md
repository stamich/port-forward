# Migration from Akka to Apache Pekko

## Summary

Milestone 0.2.0 replaces the Akka 2.8 runtime used by 0.1.1 with Apache Pekko
1.7.0 while preserving the reactive TCP architecture.

## Dependency mapping

| 0.1.1 | 0.2.0 |
|---|---|
| `com.typesafe.akka:akka-actor` | `org.apache.pekko:pekko-actor` |
| `com.typesafe.akka:akka-stream` | `org.apache.pekko:pekko-stream` |
| `com.typesafe.akka:akka-slf4j` | `org.apache.pekko:pekko-slf4j` |

## Import mapping

| Akka | Pekko |
|---|---|
| `akka.Done` | `org.apache.pekko.Done` |
| `akka.NotUsed` | `org.apache.pekko.NotUsed` |
| `akka.actor.ActorSystem` | `org.apache.pekko.actor.ActorSystem` |
| `akka.actor.CoordinatedShutdown` | `org.apache.pekko.actor.CoordinatedShutdown` |
| `akka.stream.*` | `org.apache.pekko.stream.*` |
| `akka.stream.scaladsl.*` | `org.apache.pekko.stream.scaladsl.*` |
| `akka.util.ByteString` | `org.apache.pekko.util.ByteString` |

## Configuration namespace

Akka runtime configuration:

```hocon
akka { ... }
```

became:

```hocon
pekko { ... }
```

SLF4J logger classes moved to the `org.apache.pekko` namespace.

## What did not change

The conceptual data path remains:

```text
TCP listener
 -> IncomingConnection
 -> stream flow
 -> outgoing TCP connection
 -> target
```

The `Forwarder` abstraction introduced in 0.1.1 prevented this infrastructure
migration from leaking into domain or service code.

## Additional changes made during migration

The migration also provided a controlled opportunity to:

- standardize JDK 21,
- upgrade Scala 2.13.16 -> 2.13.18,
- move lifecycle cleanup to `CoordinatedShutdown`,
- simplify mutable lifecycle state,
- add typed application exceptions,
- expand integration tests,
- add `scalafmt` to CI.

These are runtime-hardening changes, not new forwarding features.
