package software.amazon.app.kmplogger

/**
 * Defines the contract for implementing custom logging behaviors across different platforms.
 *
 * This interface allows for flexible logging implementations that can:
 * - Write to different output destinations (console, file, network, etc.)
 * - Format messages in custom ways
 * - Filter or transform logs based on criteria
 * - Implement platform-specific logging mechanisms
 *
 * Example implementation:
 * ```
 * class FileLoggingStrategy : LoggingStrategy {
 *     override fun logMessage(logLevel: LogLevel, tag: String, message: String) {
 *         val formattedMessage = "[$logLevel] ($tag): $message"
 *         writeToFile(formattedMessage)
 *     }
 * }
 * ```
 *
 * @see DefaultLoggingStrategy for the default console-based implementation
 * @see LogLevel for available logging severity levels
 */
public interface LoggingStrategy {
    /**
     * Processes and outputs a log message according to the implementation's strategy.
     *
     * @param logLevel The severity level of the log message.
     * @param tag The tag identifying the source of the log message.
     * @param message The log message content.
     * @param throwable An optional throwable associated with the log message.
     *
     * Implementation considerations:
     * - Thread safety should be handled by the implementation if needed
     * - Long messages might need special handling depending on the output destination
     * - Implementations should handle potential errors gracefully
     * - Consider performance implications for high-volume logging
     */
    public fun logMessage(
        logLevel: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )
}
