# Changelog

All notable project changes are documented in this file.

## 0.2.0 - 2026-09-28

### Changed

- Migrated the reactive runtime from Akka 2.8.8 to Apache Pekko 1.7.0.
- Upgraded Scala from 2.13.16 to 2.13.18.
- Standardized local, CI and container runtime expectations on JDK 21.
- Centralized dependency versions and coordinates in
  `project/Dependencies.scala`.
- Replaced all `akka.*` imports with `org.apache.pekko.*`.
- Replaced the `akka {}` HOCON runtime namespace with `pekko {}`.
- Reworked `TcpForwarder` lifecycle into one coherent internal state model:
  `Idle`, `Binding`, `Bound`, `Unbinding`.
- Replaced combined `AtomicReference + synchronized` lifecycle state from
  0.1.1 with synchronized transitions over one state value.
- Moved runtime cleanup to Pekko `CoordinatedShutdown`.
- Switched application/infrastructure logging to SLF4J while retaining Pekko's
  SLF4J bridge for framework logs.
- Changed HOCON forwarding structure from flat `local-*`/`remote-*` keys to
  nested `listen` and `target` sections.
- Versioned the assembly artifact as `port-forward-server-0.2.0.jar`.
- Updated Docker runtime to Java 21.
- Updated CI to check formatting before tests and assembly.

### Added

- `Version` application metadata object.
- `PortForwardException` base type.
- `ConfigurationLoadException`.
- `InvalidConfigurationException`.
- `ForwarderStateException`.
- `BindFailedException`.
- CLI `--version`.
- CLI `--validate [config-file]`.
- IPv6-safe `Endpoint.toString`.
- Per-connection numeric IDs in accepted-connection logs.
- `project/Dependencies.scala`.
- `.scalafmt.conf`.
- `sbt-scalafmt` CI quality gate.
- Endpoint rendering tests.
- lifecycle integration tests.
- duplicate-start test.
- idempotent-stop test.
- bind-failure and recovery test.
- large-payload TCP integrity test.
- `docs/MIGRATION_AKKA_TO_PEKKO.md`.
- `docs/MILESTONE_0.2.0_TASKS.md`.

### Removed

- All Akka library dependencies.
- All Akka imports.
- `ConfigError`, replaced by typed application exceptions.
- 0.1.1 milestone task document from the current release tree.
- Old flat HOCON forwarding keys.
- Any remaining reliance on an explicit JVM shutdown hook in CLI code.
- Obsolete GitLab/X11/Xvfb artifacts remain absent from the project.

### Compatibility notes

- The forwarding feature remains one TCP listener -> one TCP target.
- 0.2.0 intentionally changes the configuration schema because the project is
  pre-1.0.
- UDP, TLS, multi-rule support and management/observability APIs remain outside
  this milestone.
- Scala 3 migration remains deferred.

## 0.1.1 - 2026-09-28

### Changed

- Refactored the project into explicit `domain`, `config`, `validation`,
  `network`, `service`, `runtime`, `cli` and `gui` packages.
- Introduced the `Forwarder` abstraction.
- Centralized runtime wiring and shutdown.
- Added reusable configuration validation.
- Simplified server Docker runtime.

### Added

- unit and TCP integration tests,
- architecture and milestone documentation,
- GitHub Actions CI,
- structured logging.

### Removed

- unused Cats/Cats Effect/Akka HTTP dependencies,
- X11/Xvfb container GUI emulation,
- GitLab CI configuration,
- obsolete application buffer setting.

## 0.1.0

Initial public version with TCP forwarding, CLI mode, ScalaFX GUI, Dockerfile
and GitLab CI configuration.
