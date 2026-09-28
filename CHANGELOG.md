# Changelog

All notable changes to this project are documented in this file.

## 0.1.1 - 2026-09-28

### Changed

- Refactored the project into explicit `domain`, `config`, `validation`,
  `network`, `service`, `runtime`, `cli` and `gui` packages.
- Introduced the `Forwarder` abstraction so application logic no longer depends
  directly on Akka TCP implementation details.
- Moved validation out of CLI/GUI and into `ConfigValidator`.
- Replaced nullable/implicit configuration failure behavior with
  `Either[ConfigError, ForwardingConfig]`.
- Reworked TCP listener lifecycle into `TcpForwarder`.
- Moved per-connection flow construction into `TcpConnectionFlowFactory`.
- Centralized shared ActorSystem/resource lifecycle in `ApplicationRuntime`.
- Simplified CLI parsing and added explicit help output.
- Updated GUI to delegate all networking work to `ForwardingService`.
- Replaced networking-layer `println` usage with Akka/SLF4J logging.
- Simplified Docker runtime and switched to a non-root user.
- Added GitHub Actions CI.
- Updated README with architecture, security notes, build/run instructions and
  roadmap.

### Added

- `Endpoint`
- `ForwardingStatus`
- `ConfigError`
- `ConfigLoader`
- `HoconConfigLoader`
- `ConfigValidator`
- `Forwarder`
- `TcpConnectionFlowFactory`
- `TcpForwarder`
- `ForwardingService`
- `ApplicationRuntime`
- unit tests for validation, CLI parsing and HOCON loading
- service-layer tests with a stub transport
- TCP end-to-end integration test
- `docs/ARCHITECTURE.md`
- `docs/MILESTONE_0.1.1_TASKS.md`
- `logback.xml`

### Removed

- Direct networking responsibilities from CLI and GUI code.
- Unused Cats, Cats Effect and Akka HTTP dependencies.
- Unused Akka Typed/TestKit dependencies.
- Unused JavaFX modules (`fxml`, `media`, `swing`, `web`) from the build.
- Container-side X11/Xvfb GUI emulation and generated shell entrypoint.
- Obsolete `buffer-size` configuration key because the previous implementation
  did not apply it correctly and Akka Streams already supplies backpressure.
- Legacy GitLab CI configuration in favor of repository-native GitHub Actions.

### Compatibility notes

- 0.1.1 remains on Scala 2.13.16 and Akka 2.8.8.
- The bundled/default port-forwarding semantics remain one TCP listen endpoint
  forwarding to one TCP target endpoint.
- Technology migration to Apache Pekko is intentionally deferred to 0.2.0.

## 0.1.0

Initial public version with TCP forwarding, CLI mode, ScalaFX GUI, Dockerfile
and GitLab CI configuration.
