package software.amazon.app.kmplogger

import android.util.Log

/**
 * An Android implementation for [writeLog] that delegates to [android.util.Log] for any log
 * with the given priority and tag.
 *
 * Maps the platform-independent [LogLevel] to Android's logging priorities and writes
 * the message using the appropriate log method:
 * - [LogLevel.ERROR] → [Log.e]
 * - [LogLevel.WARN] → [Log.w]
 * - [LogLevel.INFO] → [Log.i]
 * - [LogLevel.DEBUG] → [Log.d]
 * - [LogLevel.VERBOSE] → [Log.v]
 */
internal actual fun writeLog(
    priority: LogLevel,
    tag: String,
    message: String,
    throwable: Throwable?,
) {
    when (priority) {
        LogLevel.ERROR -> {
            if (throwable == null) {
                Log.e(tag, message)
            } else {
                Log.e(tag, message, throwable)
            }
        }
        LogLevel.WARN -> {
            if (throwable == null) {
                Log.w(tag, message)
            } else {
                Log.w(tag, message, throwable)
            }
        }
        LogLevel.INFO -> Log.i(tag, message)
        LogLevel.DEBUG -> Log.d(tag, message)
        LogLevel.VERBOSE -> Log.v(tag, message)
    }
}
