package software.amazon.app.kmplogger

import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

/**
 * Extension property that provides a convenient logging interface for any Kotlin class.
 *
 * This property automatically creates a [Logger] instance with a tag derived from the class name
 * of the calling context. The tag generation defaults to the class name of the log call site,
 * without any extra runtime cost ensuring no runtime overhead. This works because [logger] is
 * an extension property of [Any] and has access to `this` from which it can extract the class name.
 * In addition, there is no need to cache the logger instance, because the [Logger] class is
 * inlined.
 *
 * Features:
 * - Inline implementation for minimal overhead.
 * - Automatic zero-cost tag generation from class name.
 * - Optional tag prefix support via [Logger.tagPrefix]
 * - Platform-specific logging integration
 *
 * Example usage:
 * ```
 * class MyService {
 *     fun doWork() {
 *         logger.d { "Starting work" }
 *         logger.i { "Work completed" }
 *         logger.e(exception) { "Authentication failed" }
 *     }
 * }
 * ```
 *
 * Note: For standalone functions or contexts without a class reference,
 * create a [Logger] instance directly with an explicit tag.
 *
 * Example usage in standalone functions:
 * ```
 * fun processData() {
 *     Logger("DataProcessor").d { "Processing started" }
 * }
 * ```
 * @see Logger For the underlying logging implementation.
 */
public val Any.logger: Logger get() = Logger(this.getLogTagFromClassName())

/**
 * Access to logger from lambda with receiver or coroutine context.
 *
 * This property is deprecated because it incorrectly captures the receiver's class
 * instead of the desired outer class context. To properly log from within a lambda
 * with receiver or coroutine, use qualified 'this' with the outer class name.
 *
 * Example:
 * ```
 * class MyClass {
 *     val greet: String.() -> Unit = {
 *         // Don't use: logger.d { "message" }
 *         // Instead use:
 *         this@MyClass.logger.d { "message" }
 *     }
 *
 *     coroutineScope.launch {
 *         // Don't use: logger.d { "message" }
 *         // Instead use:
 *         this@MyClass.logger.d { "message" }
 *     }
 * }
 * ```
 */
@Suppress("UnusedReceiverParameter")
@Deprecated(
    "Use this@<outerclassname>.logger instead",
    ReplaceWith("this@<outerclassname>.logger"),
    level = DeprecationLevel.ERROR,
)
public val CoroutineScope.logger: Nothing get() = error("Not implemented call with this@outerclassName")

/**
 * Creating a logger for another logger is redundant. Remove this `logger` call.
 */
@Suppress("UnusedReceiverParameter")
@Deprecated(
    message = "Redundant logger call.",
    ReplaceWith("this"),
    level = DeprecationLevel.ERROR,
)
public val Logger.logger: Nothing get() = error("Redundant logger call.")

/**
 * Generates a formatted log tag from the calling class's name with optional prefix.
 *
 * Tag formation rules:
 * 1. Extracts class name using [KClass.simpleName]
 * 2. Falls back to [KClass.qualifiedName] if simple name is unavailable
 * 3. Removes inner class notation (text after '$')
 * 4. Removes package prefix for brevity
 * 5. Optionally prepends [Logger.tagPrefix]
 *
 * Examples:
 * Class: com.example.MainActivity
 * - Without prefix: "MainActivity"
 * - With prefix "MyApp": "MyApp: MainActivity"
 * - Inner class: "OuterClass.InnerClass" (instead of "OuterClass$InnerClass")
 * - Local class: "OuterClass.LocalClass" (instead of "OuterClass$functionName$LocalClass")
 * - Anonymous object from class: "OuterClass" (instead of "OuterClass$functionName$1")
 *
 * @return Formatted tag string for logging.
 * @see Logger.tagPrefix For tag prefix configuration
 */
private fun Any.getLogTagFromClassName(): String {
    val simpleClassName = this::class.simpleName
    val fullName = this::class.toString()
    val qualifiedName = this::class.qualifiedName ?: fullName
    val className =
        when {
            // Anonymous objects have null simple name
            simpleClassName == null -> outerClassForAnonymous(fullName)
            // Named classes (regular, inner, or local)
            else -> resolveFullClassName(simpleClassName, fullName, qualifiedName)
        }
    // Add configured prefix if present
    return Logger.tagPrefix?.let { "$it$className" } ?: className
}

/**
 * Fallback method for getting class name when simple name is not available.
 */
private fun outerClassForAnonymous(fullName: String): String {
    val lastDot = fullName.lastIndexOf('.')
    return if (lastDot == -1) {
        fullName
    } else {
        val dollarIndex = fullName.indexOf('$', lastDot)
        if (dollarIndex == -1) {
            fullName.substring(lastDot + 1)
        } else {
            fullName.substring(lastDot + 1, dollarIndex)
        }
    }
}

/**
 * Resolves the full class name including outer class if present.
 */
private fun resolveFullClassName(
    simpleName: String,
    fullName: String,
    qualifiedName: String,
): String {
    val outerName = getOuterClassNameIfPresent(fullName, qualifiedName)
    return when {
        outerName.isNullOrEmpty() -> simpleName
        outerName == simpleName -> simpleName
        else -> "$outerName.$simpleName"
    }
}

/**
 * Extracts the outer class name if present.
 * Handles both JVM and Native style inner classes.
 *
 * Platform-specific cases:
 * 1. JVM Style (using $):
 *    - "com.example.OuterClass$InnerClass"
 *    - Extracts "OuterClass"
 *
 * 2. Native Style (using .):
 *    - "com.example.OuterClass.InnerClass"
 *    - Must check if segment is actually a class (starts with uppercase)
 */
private fun getOuterClassNameIfPresent(
    fullName: String,
    qualifiedName: String,
): String? {
    val dollarIndex = fullName.indexOf('$')
    return if (dollarIndex == -1) {
        // Native style or not an inner Class
        val segments = qualifiedName.split('.')
        val innerClassIndex = segments.lastIndex
        if (innerClassIndex == -1) {
            null
        } else {
            val potentialOuterClass = segments[innerClassIndex - 1]
            if (potentialOuterClass.first().isUpperCase()) {
                potentialOuterClass
            } else {
                null
            }
        }
    } else {
        // JVM style inner class
        fullName
            .substringAfterLast('.')
            .substringBefore('$')
            .trim()
    }
}
