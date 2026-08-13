package app.lineo.engine.golden

import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.Engine
import app.lineo.engine.EvalContext
import app.lineo.engine.Quantity
import app.lineo.engine.err
import app.lineo.engine.ok
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Locale

/**
 * The harness is written before the engine (P0-04), so it needs its own tests: it defines
 * what "correct" means for every later task.
 */
class GoldenHarnessTest {
    private val fixtureName = "sample-golden.txt"
    private val fixture: String = requireNotNull(
        javaClass.classLoader.getResource("harness/$fixtureName"),
    ).readText()

    private val cases = GoldenFileParser.parse(fixtureName, fixture)

    @Test
    fun `parser skips comments and reads every case`() {
        assertEquals(4, cases.size)
    }

    @Test
    fun `parser reports the golden line number, not the case index`() {
        assertEquals(listOf(4, 5, 6, 8), cases.map { it.lineNumber })
    }

    @Test
    fun `locale directive applies until it changes`() {
        assertEquals(Locale.forLanguageTag("en-US"), cases[0].locale)
        assertEquals(Locale.forLanguageTag("de-DE"), cases.last().locale)
    }

    @Test
    fun `expectation without a bang is a value`() {
        assertEquals(Expectation.Value("14"), cases[0].expectation)
    }

    @Test
    fun `expectation with a bang is an error type`() {
        assertEquals(Expectation.Error("DivisionByZero", span = null), cases[1].expectation)
    }

    @Test
    fun `expectation with a span asserts the span`() {
        assertEquals(Expectation.Error("UnknownIdentifier", span = 0..2), cases[2].expectation)
    }

    @Test
    fun `a malformed golden line names the file and line`() {
        val failure = assertThrows<IllegalStateException> {
            GoldenFileParser.parse("broken.txt", "# header\n2+2 is four\n")
        }
        assertTrue(failure.message.orEmpty().startsWith("broken.txt:2:"), failure.message)
    }

    @Test
    fun `a malformed span names the file and line`() {
        val failure = assertThrows<IllegalArgumentException> {
            GoldenFileParser.parse("broken.txt", "x | !Syntax@one..two\n")
        }
        assertTrue(failure.message.orEmpty().startsWith("broken.txt:1:"), failure.message)
    }

    @Test
    fun `failure against the stub evaluator points at the offending golden line`() {
        val failure = GoldenRunner.run(cases[0]) { source, context -> Engine.evaluate(source, context) }

        assertNotNull(failure)
        assertTrue(failure.orEmpty().startsWith("$fixtureName:4:"), failure)
        assertTrue(failure.orEmpty().contains("expected 14"), failure)
    }

    @Test
    fun `a matching value passes`() {
        val failure = GoldenRunner.run(cases[0]) { _, _ -> Quantity(BigDecimal("14")).ok() }

        assertNull(failure)
    }

    @Test
    fun `a matching error type passes`() {
        val failure = GoldenRunner.run(cases[1]) { _, _ -> CalcError.DivisionByZero.err() }

        assertNull(failure)
    }

    @Test
    fun `a wrong span fails and reports both spans`() {
        val failure = GoldenRunner.run(cases[2]) { _, _ ->
            CalcError.UnknownIdentifier(name = "sni", suggestion = "sin", span = 0..3).err()
        }

        assertTrue(failure.orEmpty().contains("@0..2"), failure)
        assertTrue(failure.orEmpty().contains("@0..3"), failure)
    }

    @Test
    fun `an evaluator that throws is reported as a failure, not propagated`() {
        val failure = GoldenRunner.run(cases[0]) { _: String, _: EvalContext ->
            throw IllegalStateException("boom")
        }

        assertTrue(failure.orEmpty().contains("threw IllegalStateException"), failure)
    }

    @Test
    fun `the stub evaluator returns a result rather than throwing`() {
        val result = Engine.evaluate("5 +")

        assertTrue(result is CalcResult.Err)
    }
}
