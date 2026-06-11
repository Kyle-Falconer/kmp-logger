# KMPLogger

KMPLogger is a Kotlin Multiplatform (KMP) logging library that provides a unified logging mechanism
for applications targeting Android, iOS, and JVM.

The logger default implementation logs to the platform's native console
(Logcat on Android, os_log on iOS, println on JVM), so if some modules use a different logger,
their logs will still show up as expected.

## Setup

### In build.gradle.kts

```kotlin
dependencies {
    implementation("software.amazon.app.kmplogger:kmp-logger-log:0.0.1")
}
```

## Usage

### Configuration

If you need to override the default configuration of the Logger, it is best done at application
startup before any logging occurs. If not configured, default values will be used. Logger can only
be configured once; subsequent calls throw `IllegalStateException`.

The logging behavior is defined by the minimum log level, a tag prefix, and the logging strategy.

Configuration options:
1. `minLogLevel`: Minimum severity threshold for processing logs
   - Default: `LogLevel.VERBOSE`
   - Messages below this level are ignored

2. `prefix`: Optional tag prefix for all log messages
   - Default: null (no prefix)
   - Format when set: "$prefix: $tag"
   - Must not be blank if provided else throws `IllegalArgumentException`

3. `logStrategy`: Defines how logs are processed and output
   - Default: `DefaultLoggingStrategy` (logs to the standard output like console or logcat)
   - Can be customized for different output destinations

```kotlin
// Basic configuration
Logger.configure(
    minLogLevel = LogLevel.INFO,
    prefix = "MyApp"
)

// Custom logging strategy
Logger.configure(
    logStrategy = FileLoggingStrategy("app.log")
)
```

### In Application Code

```kotlin
class LogTest {
    fun greet() {
        logger.v { "Hello, this is log from KMPLogger!" }
        // JVM -> VERBOSE: (LogTest) Hello, this is log from KMPLogger!
        // Android -> V/LogTest: Hello, this is log from KMPLogger!

        logger.d { "Hello, this is log from KMPLogger!" }
        // JVM -> DEBUG: (LogTest) Hello, this is log from KMPLogger!
        // Android -> D/LogTest: Hello, this is log from KMPLogger!

        logger.e(throwable = exception) { "test exception" }
        // JVM -> ERROR: (LogTest) test exception
        //        Exception: java.lang.IllegalStateException: test
        //           at ...
        // Android -> E/LogTest: test exception
        //            java.lang.IllegalStateException: test
        //               at ...
    }
}

fun standalone() {
    Logger("Standalone").i { "Hello, this is log from KMPLogger!" }
    // JVM -> INFO: (Standalone) Hello, this is log from KMPLogger!
    // Android -> I/Standalone: Hello, this is log from KMPLogger!
}
```

## Motivations

We built KMPLogger to address several needs when logging across Kotlin Multiplatform projects
targeting Android, iOS, and JVM:

- **Zero-cost when disabled.** Log messages use an inlined string-producing lambda. If logging is
  disabled for a given level, the lambda is never evaluated, so string interpolation and
  concatenation have no performance cost in production builds.

- **Automatic tag with no stacktrace.** The `logger` extension property on `Any` extracts the class
  name via `this::class` at the call site without creating a stacktrace. This gives you a meaningful
  tag for free while avoiding the overhead of stack walking that other libraries use.

- **True multiplatform, native output.** Each platform logs through its native mechanism — Logcat on
  Android, `os_log` on iOS, and `println` on JVM. This means logs integrate with each platform's
  tooling (filtering in Logcat, Console.app on macOS/iOS, etc.) without extra setup.

- **Optional by design.** KMPLogger writes to the same native output as any other logger on each
  platform. Modules that use a different logging library still have their logs appear in the same
  place. There's no requirement for all modules to adopt KMPLogger — it coexists peacefully.

- **Simple API surface.** One method per log level (`v`, `d`, `i`, `w`, `e`) with a lambda for the
  message. No overload explosion. Throwables are an optional named parameter rather than a separate
  set of methods.

- **Pluggable strategy.** The `LoggingStrategy` interface lets you redirect logs to files, remote
  services, or custom formatters without changing call sites. The default writes to the platform
  console.

- **Configure once, use everywhere.** Global configuration (minimum level, tag prefix, strategy)
  is set once at app startup and enforced across all loggers. Attempting to reconfigure throws,
  preventing accidental mid-run changes that could cause inconsistent behavior.

- **Value class implementation.** `Logger` is a `@JvmInline value class` wrapping a tag string, so
  creating logger instances allocates nothing on the heap. This makes the `Any.logger` pattern
  viable even in hot paths.
- 
## Contributing
See [CONTRIBUTING](CONTRIBUTING) for more information.

## License

This project is licensed under the Apache-2.0 License.
