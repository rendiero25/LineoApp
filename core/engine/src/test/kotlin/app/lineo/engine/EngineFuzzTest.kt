package app.lineo.engine

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.time.Duration
import java.util.Locale

/**
 * The fuzzing contract of `docs/GRAMMAR.md` §6: `evaluate()` always returns a
 * [CalcResult], never throws, and never hangs. One second per input.
 *
 * A parser that survives arbitrary input is the foundation everything else rests on. If a
 * change breaks this test, the change is wrong — the test is not.
 */
class EngineFuzzTest {
    @Test
    fun `random input never throws and never hangs`() {
        FuzzInputs.randomStrings().forEach(::check)
    }

    @Test
    fun `deeply nested parentheses never throw`() {
        FuzzInputs.DEEP_NESTING.forEach(::check)
    }

    @Test
    fun `huge exponents never throw`() {
        FuzzInputs.HUGE_EXPONENTS.forEach(::check)
    }

    @Test
    fun `mixed locale separators never throw`() {
        FuzzInputs.LOCALES.forEach { locale ->
            FuzzInputs.MIXED_SEPARATORS.forEach { input -> check(input, locale) }
        }
    }

    @Test
    fun `well formed looking garbage never throws`() {
        (FuzzInputs.WELL_FORMED_LOOKING + FuzzInputs.LONG_INPUTS).forEach(::check)
    }

    @Test
    fun `built-in functions never throw on hostile arguments`() {
        FuzzInputs.HOSTILE_ARGUMENTS.forEach(::check)
    }

    @Test
    fun `built-in functions never throw in any angle mode`() {
        AngleMode.entries.forEach { mode ->
            FuzzInputs.HOSTILE_ARGUMENTS.forEach { input ->
                val result = assertTimeoutPreemptively(TIMEOUT, "engine hung on: $input in $mode") {
                    Engine.evaluate(input, EvalContext(angleMode = mode))
                }
                assertNotNull(result, "engine returned null for: $input in $mode")
            }
        }
    }

    private fun check(input: String, locale: Locale = Locale.US) {
        val result = assertTimeoutPreemptively(TIMEOUT, "engine hung on: ${input.take(80)}") {
            Engine.evaluate(input, EvalContext(locale = locale))
        }
        assertNotNull(result, "engine returned null for: ${input.take(80)}")
    }

    private companion object {
        val TIMEOUT: Duration = Duration.ofSeconds(1)
    }
}
