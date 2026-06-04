package software.amazon.app.kmplogger

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * A logging strategy implementation for testing purposes that captures log entries in memory.
 * This strategy is thread-safe and prevents external modification of the stored logs.
 */
class TestLoggingStrategy : LoggingStrategy {
    data class LogEntry(
        val logLevel: LogLevel,
        val tag: String,
        val throwable: Throwable?,
        val message: String,
    )

    private val lock = SynchronizedObject()
    private val logEntries = mutableListOf<LogEntry>()
    internal val logs: List<LogEntry>
        get() = synchronized(lock) { logEntries.toList() }

    override fun logMessage(
        logLevel: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        synchronized(lock) {
            logEntries += LogEntry(logLevel, tag, throwable, message)
        }
    }

    fun clear() {
        synchronized(lock) {
            logEntries.clear()
        }
    }
}
