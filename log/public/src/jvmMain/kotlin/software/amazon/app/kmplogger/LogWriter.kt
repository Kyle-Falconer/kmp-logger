package software.amazon.app.kmplogger

/**
 * Desktop/JVM-specific implementation that writes log messages to the console using [println].
 *
 * Formats the output as: "{PRIORITY_LEVEL}: (tag) message"
 * Example: "DEBUG: (MyClass) Hello, World!"
 */
internal actual fun writeLog(
    priority: LogLevel,
    tag: String,
    message: String,
    throwable: Throwable?,
) {
    val logMessage =
        buildString {
            append(message)
            if (throwable != null) {
                append("\nException: ${throwable.stackTraceToString()}")
            }
        }
    println("${priority.name}: ($tag) $logMessage")
}
