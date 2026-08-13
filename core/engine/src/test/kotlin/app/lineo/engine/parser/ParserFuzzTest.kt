package app.lineo.engine.parser

import app.lineo.engine.FuzzInputs
import app.lineo.engine.lexer.Lexer
import app.lineo.engine.lexer.NumberFormatProfile
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.time.Duration
import java.util.Locale

/**
 * The lexer and parser take the same beating as the whole engine (`docs/GRAMMAR.md` §6),
 * so a parser regression is caught here even while later stages are still stubs.
 */
class ParserFuzzTest {
    @Test
    fun `random input parses to a result, never an exception`() {
        FuzzInputs.randomStrings().forEach(::check)
    }

    @Test
    fun `structural edge cases parse to a result`() {
        val inputs = FuzzInputs.WELL_FORMED_LOOKING +
            FuzzInputs.DEEP_NESTING +
            FuzzInputs.HUGE_EXPONENTS +
            FuzzInputs.LONG_INPUTS
        inputs.forEach(::check)
    }

    @Test
    fun `mixed locale separators parse to a result in every locale`() {
        FuzzInputs.LOCALES.forEach { locale ->
            FuzzInputs.MIXED_SEPARATORS.forEach { input -> check(input, locale) }
        }
    }

    private fun check(input: String, locale: Locale = Locale.US) {
        val result = assertTimeoutPreemptively(TIMEOUT, "parser hung on: ${input.take(80)}") {
            val tokens = Lexer(NumberFormatProfile.forLocale(locale)).tokenize(input)
            Parser(tokens, knownFunctions = KNOWN_FUNCTIONS).parse()
        }
        assertNotNull(result, "parser returned null for: ${input.take(80)}")
    }

    private companion object {
        val TIMEOUT: Duration = Duration.ofSeconds(1)
        val KNOWN_FUNCTIONS = setOf("sin", "cos", "tan", "log", "ln", "sqrt", "max", "min")
    }
}
