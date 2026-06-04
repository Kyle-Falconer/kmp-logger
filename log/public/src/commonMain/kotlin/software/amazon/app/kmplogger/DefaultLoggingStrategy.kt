package software.amazon.app.kmplogger

/**
 * Default implementation of [LoggingStrategy] that delegates to platform-specific logging
 * mechanisms.
 *
 * This strategy provides consistent logging behavior across different platforms while utilizing
 * the most appropriate native logging system for each platform:
 * - Android: Logs to Logcat using `android.util.Log`
 * - iOS: Uses the native `_os_log_internal` API
 * - JVM: Writes to standard output using [println]
 */
public class DefaultLoggingStrategy : LoggingStrategy {
    override fun logMessage(
        logLevel: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        writeLog(logLevel, tag, message, throwable)
    }
}

/**
 * Platform-specific logging function that writes messages to the appropriate logging system.
 *
 * Implementation details by platform:
 * - Android: Writes to Logcat with corresponding priority levels
 * - iOS: Utilizes _os_log_internal with mapped log types
 * - JVM: Outputs formatted messages to standard output using println
 *
 * Note: The actual implementation and any platform-specific limitations or behaviors
 * are defined in the platform-specific actual functions.
 */
internal expect fun writeLog(
    priority: LogLevel,
    tag: String,
    message: String,
    throwable: Throwable?,
)
