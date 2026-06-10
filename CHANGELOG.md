# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

### Fixed

### Security

### Other Notes & Contributions

## [0.0.1] - 2026-6-10

- Initial release of KMPLogger
- `Logger` value class with zero-allocation logging
- `LogLevel` enum (VERBOSE, DEBUG, INFO, WARN, ERROR)
- `LoggingStrategy` interface for custom logging implementations
- `DefaultLoggingStrategy` with platform-specific implementations
- `Any.logger` extension property for automatic tag generation
- Platform support: Android (Logcat), iOS (os_log), JVM (println)
- Configurable minimum log level
- Optional tag prefix support
- Thread-safe configuration

[Unreleased]: https://github.com/amzn/kmp-logger/compare/0.0.1...HEAD
[0.0.1]: https://github.com/amzn/kmp-logger/compare/0.0.1

