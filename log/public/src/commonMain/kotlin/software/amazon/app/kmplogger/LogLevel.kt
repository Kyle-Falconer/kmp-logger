package software.amazon.app.kmplogger

/**
 * Platform-independent logging severity levels that map to native logging priorities in different
 * platforms without a direct import.
 *
 * This enum provides a consistent logging interface across different platforms while mapping to
 * their respective native logging systems:
 * - Android: Maps to android.util.Log constants
 * - iOS: Maps to os_log types
 * - JVM: Used for formatted console output
 *
 * Levels are ordered by increasing severity:
 * [VERBOSE] (1) → [DEBUG] (2) → [INFO] (3) → [WARN] (4) → [ERROR] (5)
 *
 * This ensures that the log level is consistent across platforms and can be used in the
 * logger without importing the platform-specific Log class.
 */
public enum class LogLevel(
    /**
     * Integer value representing the log severity level.
     * Higher values indicate higher severity.
     */
    public val level: Int,
) {
    /**
     * Verbose logging for detailed debugging information.
     * Lowest priority, typically used for extensive debugging details.
     * Maps to:
     * - Android: Log.VERBOSE
     * - iOS: OS_LOG_TYPE_DEBUG
     */
    VERBOSE(1),

    /**
     * Debug logging for development and troubleshooting.
     * Used for debugging information that should not appear in release builds.
     * Maps to:
     * - Android: Log.DEBUG
     * - iOS: OS_LOG_TYPE_DEBUG
     */
    DEBUG(2),

    /**
     * Informational logging for tracking normal application flow.
     * Used for noteworthy events that aren't errors.
     * Maps to:
     * - Android: Log.INFO
     * - iOS: OS_LOG_TYPE_INFO
     */
    INFO(3),

    /**
     * Warning logging for potentially harmful situations.
     * Used for recoverable issues or unexpected behavior.
     * Maps to:
     * - Android: Log.WARN
     * - iOS: OS_LOG_TYPE_DEFAULT
     */
    WARN(4),

    /**
     * Error logging for serious problems.
     * Highest priority, used for errors that need immediate attention.
     * Maps to:
     * - Android: Log.ERROR
     * - iOS: OS_LOG_TYPE_ERROR
     */
    ERROR(5),
}
