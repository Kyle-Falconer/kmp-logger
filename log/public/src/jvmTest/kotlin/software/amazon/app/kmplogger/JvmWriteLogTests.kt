package software.amazon.app.kmplogger

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class JvmWriteLogTests {
    private val outputStream = ByteArrayOutputStream()
    private val originalOut = System.out

    @BeforeTest
    fun setUp() {
        System.setOut(PrintStream(outputStream))
    }

    @AfterTest
    fun tearDown() {
        System.setOut(originalOut)
        outputStream.reset()
    }

    @Test
    fun `test basic log message format`() {
        writeLog(LogLevel.DEBUG, "TestTag", "Hello World", null)

        assertThat(
            "DEBUG: (TestTag) Hello World",
        ).isEqualTo(getCapturedOutput())
    }

    @Test
    fun `test all log levels`() {
        val message = "Test message"
        val tag = "TestTag"

        LogLevel.entries.forEach { level ->
            outputStream.reset()
            writeLog(level, tag, message, null)
            assertThat(
                "${level.name}: ($tag) $message",
            ).isEqualTo(getCapturedOutput())
        }
    }

    @Test
    fun `test message with exception`() {
        val exception = RuntimeException("Test exception")
        writeLog(LogLevel.ERROR, "TestTag", "Failed", exception)

        val expected =
            buildString {
                append("ERROR: (TestTag) Failed")
                append("\nException: ")
                append(exception.stackTraceToString())
            }.trim()

        assertThat(expected).isEqualTo(getCapturedOutput())
    }

    @Test
    fun `test empty message`() {
        writeLog(LogLevel.INFO, "TestTag", "", null)
        assertThat(
            getCapturedOutput(),
        ).isEqualTo("INFO: (TestTag)")
    }

    @Test
    fun `test empty tag`() {
        writeLog(LogLevel.WARN, "", "Message", null)
        assertThat(
            "WARN: () Message",
        ).isEqualTo(getCapturedOutput())
    }

    @Test
    fun `test message with special characters`() {
        writeLog(
            LogLevel.DEBUG,
            "TestTag",
            "Message with\nnewline and\tspecial chars!@#$%^&*()",
            null,
        )
        assertThat(
            "DEBUG: (TestTag) Message with\nnewline and\tspecial chars!@#$%^&*()",
        ).isEqualTo(getCapturedOutput())
    }

    @Test
    fun `test exception with nested cause`() {
        val innerException = IllegalArgumentException("Inner error")
        val outerException = RuntimeException("Outer error", innerException)

        writeLog(LogLevel.ERROR, "TestTag", "Failed", outerException)

        val output = getCapturedOutput()
        assertThat(output.startsWith("ERROR: (TestTag) Failed\nException: ")).isTrue()
        assertThat(output.contains("RuntimeException: Outer error")).isTrue()
        assertThat(
            output.contains("Caused by: java.lang.IllegalArgumentException: Inner error"),
        ).isTrue()
    }

    @Test
    fun `test long message`() {
        val longMessage = "a".repeat(5000)
        writeLog(LogLevel.INFO, "TestTag", longMessage, null)
        assertThat(
            "INFO: (TestTag) $longMessage",
        ).isEqualTo(getCapturedOutput())
    }

    @Test
    fun `test multiple log calls`() {
        writeLog(LogLevel.DEBUG, "Tag1", "Message1", null)
        val output1 = getCapturedOutput()
        outputStream.reset()

        writeLog(LogLevel.ERROR, "Tag2", "Message2", null)
        val output2 = getCapturedOutput()

        assertThat("DEBUG: (Tag1) Message1").isEqualTo(output1)
        assertThat("ERROR: (Tag2) Message2").isEqualTo(output2)
    }

    @Test
    fun `test exception without message`() {
        val exception = RuntimeException()
        writeLog(LogLevel.ERROR, "TestTag", "Failed", exception)

        val output = getCapturedOutput()
        assertThat(output.startsWith("ERROR: (TestTag) Failed\nException: ")).isTrue()
        assertThat(output.contains("RuntimeException")).isTrue()
    }

    private fun getCapturedOutput(): String = outputStream.toString().trim()
}
