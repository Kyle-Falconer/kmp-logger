package software.amazon.app.kmplogger

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class LoggerTest {
    private val tagPrefix = "UnitTests"
    private lateinit var testLoggingStrategy: TestLoggingStrategy

    private inner class InnerTestClass {
        fun log() {
            logger.i { "Inner class says Hi" }
        }
    }

    private open class AnotherLocalClass

    @BeforeTest
    fun setup() {
        testLoggingStrategy = TestLoggingStrategy()
        Logger.reset()
        testLoggingStrategy.clear()
    }

    @AfterTest
    fun tearDown() {
        Logger.reset()
        testLoggingStrategy.clear()
    }

    @Test
    fun `test is loggable with different log levels`() {
        Logger.configure(
            minLogLevel = LogLevel.INFO,
            logStrategy = testLoggingStrategy,
        )
        logger.v { "Yo" }
        assertThat(testLoggingStrategy.logs.size).isEqualTo(0)
        logger.d { "Yo" }
        assertThat(testLoggingStrategy.logs.size).isEqualTo(0)
        logger.i { "Info" }
        assertThat(testLoggingStrategy.logs.size).isEqualTo(1)
        logger.w { "Warn" }
        assertThat(testLoggingStrategy.logs.size).isEqualTo(2)
        logger.e { "Error" }
        assertThat(testLoggingStrategy.logs.size).isEqualTo(3)
    }

    @Test
    fun `test Logger can only be configured once`() {
        Logger.configure(
            minLogLevel = LogLevel.INFO,
            logStrategy = testLoggingStrategy,
        )
        assertThat(testLoggingStrategy.logs.size).isEqualTo(0)
        assertFailure {
            Logger.configure(
                minLogLevel = LogLevel.ERROR,
            )
        }.hasMessage(
            "Logger has already been configured. " +
                "Configuration can only be set once at startup.",
        )
        assertThat(Logger.minLevel).isEqualTo(LogLevel.INFO)
    }

    @Test
    fun `test Logger cannot be configured with blank tag prefix`() {
        assertFailure {
            Logger.configure(
                prefix = "",
                minLogLevel = LogLevel.ERROR,
                logStrategy = testLoggingStrategy,
            )
        }.hasMessage("Prefix must not be blank if provided.")
        assertThat(Logger.minLevel).isEqualTo(LogLevel.VERBOSE)
    }

    @Test
    fun `test set Tag Prefix changes tag prefix`() {
        Logger.configure(
            prefix = tagPrefix,
            minLogLevel = LogLevel.INFO,
            logStrategy = testLoggingStrategy,
        )
        logger.i { "Hi" }
        assertThat(testLoggingStrategy.logs[0].logLevel).isEqualTo(LogLevel.INFO)
        assertThat(testLoggingStrategy.logs[0].tag).isEqualTo("$tagPrefix: LoggerTest")
    }

    @Test
    fun `message lambda isn't invoked when log level is below minimum level`() {
        Logger.configure(
            prefix = tagPrefix,
            minLogLevel = LogLevel.INFO,
            logStrategy = testLoggingStrategy,
        )
        var count = 0

        logger.v { "Yo${++count}" }

        assertThat(count).isEqualTo(0)
        assertThat(testLoggingStrategy.logs.isEmpty()).isTrue()
    }

    @Test
    fun `log logs message from lambda`() {
        Logger.configure(
            logStrategy = testLoggingStrategy,
        )
        var count = 0
        logger.d { "Hi ${++count}" }
        assertThat(count).isEqualTo(1)
        assertThat(testLoggingStrategy.logs.size).isEqualTo(1)
        testLoggingStrategy.logs[0].also { log ->
            assertThat(log.tag).isEqualTo("LoggerTest")
            assertThat(log.message).isEqualTo("Hi 1")
        }
    }

    @Test
    fun `log tag overriding passes tag to logger`() {
        Logger.configure(
            prefix = tagPrefix,
            logStrategy = testLoggingStrategy,
        )
        logger.d(tag = "Bonjour") { "Hi" }
        testLoggingStrategy.logs[0].also { log ->
            assertThat(log.tag).isEqualTo("Bonjour")
            assertThat(log.message).isEqualTo("Hi")
        }
    }

    @Test
    fun `log captures tag from outer context class name`() {
        Logger.configure(
            logStrategy = testLoggingStrategy,
        )
        logger.d { "Hi" }
        assertThat(testLoggingStrategy.logs.size).isEqualTo(1)
        with(testLoggingStrategy.logs[0]) {
            assertThat(tag).isEqualTo("LoggerTest")
            assertThat(message).isEqualTo("Hi")
        }
    }

    @Test
    fun `message lambda isn't invoked when not loggable`() {
        Logger.configure(
            minLogLevel = LogLevel.INFO,
            logStrategy = testLoggingStrategy,
        )
        var count = 0
        logger.d { "Yo${++count}" }
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `Throwable has stacktrace logged`() {
        Logger.configure(
            logStrategy = testLoggingStrategy,
        )
        val exception = Exception("test")

        logger.e(throwable = exception) { "test exception" }
        with(testLoggingStrategy.logs[0]) {
            assertThat(message).isEqualTo("test exception")
            assertThat(throwable).isEqualTo(exception)
            assertThat(logLevel).isEqualTo(LogLevel.ERROR)
        }
    }

    @Test
    fun `standalone function can log with tag`() {
        Logger.configure(
            logStrategy = testLoggingStrategy,
        )

        val tag = "Bonjour"
        val message = "Hi"
        val exception = IllegalStateException("test")

        standaloneFunctionLogs(tag = "Bonjour", message = { "Hi" }, throwable = exception)
        testLoggingStrategy.logs.forEach { current ->
            assertThat(current.logLevel.level).isGreaterThanOrEqualTo(Logger.minLevel.level)
        }
        val expectedEntries =
            listOf(
                TestLoggingStrategy.LogEntry(LogLevel.VERBOSE, tag, null, message),
                TestLoggingStrategy.LogEntry(LogLevel.DEBUG, tag, null, message),
                TestLoggingStrategy.LogEntry(LogLevel.INFO, tag, null, message),
                TestLoggingStrategy.LogEntry(LogLevel.WARN, tag, exception, message),
                TestLoggingStrategy.LogEntry(LogLevel.ERROR, tag, exception, message),
            )
        assertThat(testLoggingStrategy.logs).hasSize(expectedEntries.size)

        testLoggingStrategy.logs.zip(expectedEntries).forEach { (actual, expected) ->
            assertThat(actual.logLevel).isEqualTo(expected.logLevel)
            assertThat(actual.tag).isEqualTo(expected.tag)
            assertThat(actual.message).isEqualTo(expected.message)
            assertThat(actual.throwable).isEqualTo(expected.throwable)
        }
    }

    @Test
    fun `log captures outer this tag from lambda`() {
        Logger.configure(
            logStrategy = testLoggingStrategy,
        )
        val lambda = {
            logger.d { "Hi" }
        }
        lambda()
        with(testLoggingStrategy.logs[0]) {
            assertThat(tag).isEqualTo("LoggerTest")
            assertThat(message).isEqualTo("Hi")
        }
    }

    @Test
    fun `logcat captures outer this tag from nested lambda`() {
        Logger.configure(
            logStrategy = testLoggingStrategy,
        )
        val lambda = {
            val lambda = {
                logger.d { "Hi" }
            }
            lambda()
        }
        lambda()

        with(testLoggingStrategy.logs[0]) {
            assertThat(tag).isEqualTo("LoggerTest")
            assertThat(message).isEqualTo("Hi")
        }
    }

    @Test
    fun `log captures outer this tag from anonymous objects`() {
        Logger.configure(
            logStrategy = testLoggingStrategy,
        )
        val anonymousObject =
            object {
                fun log() {
                    logger.d { "Hi from anonymous Object" }
                }

                override fun toString(): String = "AnonymousObject override toString"
            }
        anonymousObject.log()
        with(testLoggingStrategy.logs[0]) {
            assertThat(tag).isEqualTo("LoggerTest")
            assertThat(message).isEqualTo("Hi from anonymous Object")
        }

        val anotherAnonymousObject =
            object : AnotherLocalClass() {
                fun log() {
                    logger.d { "Hi from another anonymous Object" }
                }

                override fun toString(): String = "AnotherAnonymousObject override toString"
            }
        anotherAnonymousObject.log()
        with(testLoggingStrategy.logs[1]) {
            assertThat(tag).isEqualTo("LoggerTest")
            assertThat(message).isEqualTo("Hi from another anonymous Object")
        }
    }

    @Test
    fun `log captures outer this tag from local class`() {
        Logger.configure(
            logStrategy = testLoggingStrategy,
        )

        class LocalClass {
            fun log() {
                logger.d { "Local class log" }
            }

            override fun toString(): String = "Local Class override toString"
        }
        LocalClass().log()
        with(testLoggingStrategy.logs[0]) {
            assertThat(tag).isEqualTo("LoggerTest.LocalClass")
            assertThat(message).isEqualTo("Local class log")
        }
    }

    @Test
    fun `log captures outer this tag from inner class`() {
        Logger.configure(
            logStrategy = testLoggingStrategy,
        )
        InnerTestClass().log()
        with(testLoggingStrategy.logs[0]) {
            assertThat(tag).isEqualTo("LoggerTest.InnerTestClass")
            assertThat(message).isEqualTo("Inner class says Hi")
        }
    }

    @Test
    fun `coroutine scope logger property throws error with deprecation warning`() =
        runTest {
            Logger.configure(
                logStrategy = testLoggingStrategy,
            )

            val greet: CoroutineScope.() -> Unit = {
                this@LoggerTest.logger.d { "Hello from coroutine!" }
            }

            greet()
            greet.invoke(this)

            with(testLoggingStrategy.logs[0]) {
                assertThat(tag).isEqualTo("LoggerTest")
                assertThat(message).isEqualTo("Hello from coroutine!")
            }

            with(testLoggingStrategy.logs[1]) {
                assertThat(tag).isEqualTo("LoggerTest")
                assertThat(message).isEqualTo("Hello from coroutine!")
            }
        }
}

private fun standaloneFunctionLogs(
    tag: String,
    message: () -> String,
    throwable: Throwable? = null,
) {
    Logger(tag).apply {
        v(message = message)
        d(message = message)
        i(message = message)
        w(throwable = throwable, message = message)
        e(throwable = throwable, message = message)
    }
}
