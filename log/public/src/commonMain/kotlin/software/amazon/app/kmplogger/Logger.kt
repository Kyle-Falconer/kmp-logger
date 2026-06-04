package software.amazon.app.kmplogger

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.jvm.JvmInline

/**
 * A platform-independent logging utility that provides structured logging capabilities.
 *
 * The Logger provides two main ways to log messages:
 * 1. Using the [Any.logger] extension property within classes (recommended)
 * 2. Direct instantiation for standalone functions
 *
 * Features:
 * - Multiple log levels ([LogLevel])
 * - Zero-overhead logging with inline functions
 * - Tag-based logging with optional default tags
 * - Exception handling support
 * - Configurable minimum log level
 * - Custom logging strategy support
 * - Optional tag prefixing
 *
 * Configuration (once at app startup):
 * ```
 * Logger.configure(
 *     minLogLevel = LogLevel.DEBUG,
 *     prefix = "MyApp",
 *     logStrategy = CustomLoggingStrategy()
 * )
 * ```
 *
 * Usage within classes (preferred):
 * ```
 * logger.d { "Debug message" }
 * logger.e(throwable = exception) { "Error occurred" }
 * ```
 *
 * Usage in standalone functions:
 * ```kotlin
 * fun processData() {
 *     val logger = Logger("DataProcessor")
 *     logger.d { "Processing started" }
 * }
 * ```
 *
 * Performance considerations:
 * - Message lambdas are only evaluated if the message will be logged
 * - Tag generation has zero runtime overhead
 * - Value class implementation ensures no object allocation
 *
 * Thread safety:
 * - Configuration is thread-safe
 * - Logging operations are thread-safe if the logging strategy is thread-safe
 * - Configuration values use eventual consistency for better performance
 *
 * @property defaultTag Default identifier for log messages from this logger instance.
 */
@JvmInline
public value class Logger(
    @PublishedApi
    internal val defaultTag: String,
) {
    /**
     * Logs a verbose message with the lowest priority, with optional [tag] to override
     * [defaultTag] and a [message] Lambda that produces the message to log only when the log level
     * requirement is met.
     */
    public inline fun v(
        tag: String = defaultTag,
        message: () -> String,
    ) {
        if (LogLevel.VERBOSE.level >= minLevel.level) {
            loggingStrategy.logMessage(LogLevel.VERBOSE, tag, message())
        }
    }

    /**
     * Logs a debug message for development and troubleshooting, with optional [tag] to override
     * [defaultTag] and a [message] Lambda that produces the message to log only when the log level
     * requirement is met.
     */
    public inline fun d(
        tag: String = defaultTag,
        message: () -> String,
    ) {
        if (LogLevel.DEBUG.level >= minLevel.level) {
            loggingStrategy.logMessage(LogLevel.DEBUG, tag, message())
        }
    }

    /**
     * Logs an informational message for tracking normal application flow, with optional [tag] to
     * override [defaultTag], and message Lambda that produces the [message] to log only when the
     * log level requirement is met.
     */
    public inline fun i(
        tag: String = defaultTag,
        message: () -> String,
    ) {
        if (LogLevel.INFO.level >= minLevel.level) {
            loggingStrategy.logMessage(LogLevel.INFO, tag, message())
        }
    }

    /**
     * Logs a warning message for potentially harmful situations, with optional [tag] to override
     * [defaultTag], an optional [throwable] associated with the warning, and message Lambda that
     * produces the [message] to log only when the log level requirement is met.
     */
    public inline fun w(
        tag: String = defaultTag,
        throwable: Throwable? = null,
        message: () -> String,
    ) {
        if (LogLevel.WARN.level >= minLevel.level) {
            loggingStrategy.logMessage(LogLevel.WARN, tag, message(), throwable)
        }
    }

    /**
     * Logs an error message for serious problems, with optional [tag] to override
     * [defaultTag], an optional [throwable] associated with the warning, and message Lambda that
     * produces the [message] to log only when the log level requirement is met.
     */
    public inline fun e(
        tag: String = defaultTag,
        throwable: Throwable? = null,
        message: () -> String,
    ) {
        if (LogLevel.ERROR.level >= minLevel.level) {
            loggingStrategy.logMessage(LogLevel.ERROR, tag, message(), throwable)
        }
    }

    /**
     * Thread-safe state management for the Logger configuration.
     *
     * This companion object provides centralized control over logging behavior through:
     * - Global configuration management for log levels, tag prefixes, and strategies.
     * - Thread-safe state handling using synchronization.
     * - Default logging behavior when not explicitly configured.
     *
     * The configuration is designed to be set once at application startup and
     * remains immutable afterwards to ensure consistent logging behavior.
     */
    public companion object {
        /**
         * Flag to indicate whether the logger has been configured or not.
         * Protected by [lock] to ensure thread-safe initialization.
         */
        private var isInitialized = false

        /**
         * Synchronization object for thread-safe configuration operations.
         * Used to protect shared state during initialization and updates.
         */
        private val lock = SynchronizedObject()

        /**
         * Minimum severity threshold for log processing.
         * Messages below this level will be ignored.
         */
        private var minimumLogLevel = LogLevel.VERBOSE

        /**
         * Prefix applied to all log tags.
         * When set, formats tags as "$prefix: $tag".
         */
        private var logTagPrefix: String? = null

        /**
         * Strategy implementation for log processing and output.
         * Determines how log messages are handled across platforms.
         */
        private var strategy: LoggingStrategy = DefaultLoggingStrategy()

        /**
         * Separator used between prefix and tag in log messages.
         */
        private const val TAG_SEPARATOR = ": "

        /**
         * Current minimum severity level for log processing.
         * Messages with lower severity than this level will be ignored.
         *
         * @return [LogLevel] representing the minimum severity level for logging.
         * @see LogLevel for available severity levels
         */
        @PublishedApi
        internal val minLevel: LogLevel get() = minimumLogLevel

        /**
         * Current tag prefix for log messages.
         * When non-null, all log tags will be prefixed with this value.
         *
         * Format: "$tagPrefix: $tag"
         * Example: With prefix "App" and tag "Main": "App: Main"
         *
         * @return [String] prefix for all log tags or null if not set.
         */
        @PublishedApi
        internal val tagPrefix: String? get() = logTagPrefix

        /**
         * Current strategy for processing and outputting log messages.
         * Determines how logs are handled across different platforms.
         *
         * @return [LoggingStrategy] implementation for log processing and output.
         * @see LoggingStrategy for implementation details
         * @see DefaultLoggingStrategy for default behavior
         */
        @PublishedApi
        internal val loggingStrategy: LoggingStrategy get() = strategy

        /**
         * Resets the logger to its initial default state.
         *
         * Default values:
         * - [minLevel]: [LogLevel.VERBOSE]
         * - [tagPrefix]: null
         * - [strategy]: [DefaultLoggingStrategy]
         *
         * Warning: This method is for testing purposes only and should not be used in
         * production code.
         */
        internal fun reset() {
            synchronized(lock) {
                isInitialized = false
                minimumLogLevel = LogLevel.VERBOSE
                logTagPrefix = null
                strategy = DefaultLoggingStrategy()
            }
        }

        /**
         * Configures the global logging behavior for the application.
         *
         * Configuration Timing:
         * - Best called at application startup before any logging occurs
         * - If not configured, default values will be used
         * - Can only be configured once; subsequent calls throw [IllegalStateException]
         *
         * Configuration options:
         * 1. [minLogLevel]: Minimum severity threshold for processing logs
         *    - Default: [LogLevel.VERBOSE]
         *    - Messages below this level are ignored
         *
         * 2. [prefix]: Optional tag prefix for all log messages
         *    - Default: null (no prefix)
         *    - Format when set: "$prefix: $tag"
         *    - Must not be blank if provided else throws [IllegalArgumentException]
         *
         * 3. [logStrategy]: Defines how logs are processed and output
         *    - Default: [DefaultLoggingStrategy]
         *    - Can be customized for different output destinations
         *
         * Thread Safety:
         * - Configuration is synchronized
         * - Only one thread can configure at a time
         * - Values are atomically updated
         * - Subsequent reads are eventually consistent
         *
         * Example Usage:
         * ```
         * // Basic configuration
         * Logger.configure(
         *     minLogLevel = LogLevel.INFO,
         *     prefix = "MyApp"
         * )
         *
         * // Custom logging strategy
         * Logger.configure(
         *     logStrategy = FileLoggingStrategy("app.log")
         * )
         * ```
         */
        public fun configure(
            minLogLevel: LogLevel = minLevel,
            prefix: String? = null,
            logStrategy: LoggingStrategy = strategy,
        ) {
            synchronized(lock) {
                check(!isInitialized) {
                    "Logger has already been configured. " +
                        "Configuration can only be set once at startup."
                }
                require(
                    prefix == null || prefix.isNotBlank(),
                ) { "Prefix must not be blank if provided." }

                minimumLogLevel = minLogLevel
                if (prefix != null) {
                    val logtagPrefix = "$prefix$TAG_SEPARATOR"
                    logTagPrefix = logtagPrefix
                }
                strategy = logStrategy
                isInitialized = true
            }
        }
    }
}
