package software.amazon.app.kmplogger

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ptr
import platform.darwin.OS_LOG_DEFAULT
import platform.darwin.OS_LOG_TYPE_DEBUG
import platform.darwin.OS_LOG_TYPE_DEFAULT
import platform.darwin.OS_LOG_TYPE_ERROR
import platform.darwin.OS_LOG_TYPE_INFO
import platform.darwin.__dso_handle
import platform.darwin._os_log_internal

/**
 * Apple-platform implementation (iOS and macOS) that writes log messages using the native
 * [_os_log_internal] API.
 *
 * This implementation:
 * 1. Maps Kotlin log levels to os_log types:
 *    - [LogLevel.VERBOSE], [LogLevel.DEBUG] → [OS_LOG_TYPE_DEBUG]
 *    - [LogLevel.INFO] → [OS_LOG_TYPE_INFO]
 *    - [LogLevel.WARN] → [OS_LOG_TYPE_DEFAULT]
 *    - [LogLevel.ERROR] → [OS_LOG_TYPE_ERROR]
 * 2. Uses format strings to properly handle special characters
 * 3. Includes exception details when available.
 *
 * Formats the output as: "(tag) message" or "(tag) message\nException: stacktrace"
 * and sends it to the system logging facility using the default log object ([OS_LOG_DEFAULT]).
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun writeLog(
    priority: LogLevel,
    tag: String,
    message: String,
    throwable: Throwable?,
) {
    _os_log_internal(
        dso = __dso_handle.ptr,
        log = OS_LOG_DEFAULT,
        type =
            when (priority) {
                LogLevel.VERBOSE, LogLevel.DEBUG -> OS_LOG_TYPE_DEBUG
                LogLevel.INFO -> OS_LOG_TYPE_INFO
                LogLevel.WARN -> OS_LOG_TYPE_DEFAULT
                LogLevel.ERROR -> OS_LOG_TYPE_ERROR
            },
        message =
            buildString {
                append("($tag) $message")

                if (throwable != null) {
                    appendLine()
                    append("Exception: ${throwable.stackTraceToString()}")
                }
            },
    )
}
