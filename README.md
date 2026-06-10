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
    implementation("software.amazon.app.kmplogger:kmp-logger-public:0.0.1")
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

## Contributing
See [CONTRIBUTING](CONTRIBUTING) for more information.

## License

This project is licensed under the Apache-2.0 License.
