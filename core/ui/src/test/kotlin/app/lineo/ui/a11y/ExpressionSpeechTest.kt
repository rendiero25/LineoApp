package app.lineo.ui.a11y

import app.lineo.engine.CalcResult
import app.lineo.engine.Engine
import app.lineo.engine.EvalContext
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * What TalkBack says about an expression (`docs/CONVENTIONS.md` §8).
 *
 * Every case goes through the real parser rather than a hand-built tree, because the point
 * of reading from the AST is that the *parser* decided what the characters meant — a test
 * that assembled its own tree would assert the reader against its own guess.
 */
class ExpressionSpeechTest {

    private val words = SpeechWords(
        plus = "plus",
        minus = "minus",
        times = "times",
        dividedBy = "divided by",
        modulo = "modulo",
        toThePowerOf = "to the power of",
        percentOf = "of",
        negative = "negative",
        percent = "percent",
        factorial = "factorial",
        degrees = "degrees",
        of = "of",
        and = "and",
        to = "to",
        assigned = "is",
        openBracket = "open bracket",
        closeBracket = "close bracket",
        line = "line",
    )

    @Test
    fun `a power is spoken, not spelled`() {
        assertEquals("2 to the power of 3", spoken("2^3"))
    }

    @Test
    fun `precedence is heard without brackets being said`() {
        // Nothing is bracketed here, because nothing was: the tree already says that the
        // multiplication happens first, and saying so out loud would only add noise.
        assertEquals("1 plus 2 times 3", spoken("1 + 2 * 3"))
    }

    @Test
    fun `a bracket that changes the answer is said`() {
        assertEquals("open bracket 1 plus 2 close bracket times 3", spoken("(1 + 2) * 3"))
    }

    @Test
    fun `the three meanings of percent are told apart`() {
        // docs/GRAMMAR.md §3.6: the same character, three readings, and only the parser knows
        // which is which — the reason this reads the tree and not the text.
        assertEquals("50 percent", spoken("50%"))
        assertEquals("50 percent of 80", spoken("50% of 80"))
        assertEquals("200 plus 10 percent", spoken("200 + 10%"))
    }

    @Test
    fun `a call is spoken as a call, whatever its arity`() {
        assertEquals("sin of 30", spoken("sin(30)"))
        assertEquals("max of 1 and 5", spoken("max(1, 5)"))
    }

    @Test
    fun `implicit multiplication is still multiplication out loud`() {
        assertEquals("2 times open bracket 1 plus 3 close bracket", spoken("2(1 + 3)"))
    }

    @Test
    fun `a negation says which number it is on`() {
        assertEquals("negative 5 plus 3", spoken("-5 + 3"))
    }

    @Test
    fun `a conversion reads the way it is written`() {
        assertEquals("5 km to mi", spoken("5 km to mi"))
    }

    @Test
    fun `a quantity is not read as a multiplication`() {
        // The parser reaches `5 km` as an implicit multiplication by a name, which is the
        // machinery and not what the user wrote.
        assertEquals("5 km", spoken("5 km"))
        assertEquals("5 km plus 300 m", spoken("5 km + 300 m"))
    }

    @Test
    fun `a labelled line names what it defines`() {
        assertEquals("total is 2 plus 3", spoken("total = 2 + 3"))
    }

    @Test
    fun `a line reference is read as a line`() {
        assertEquals("line 3 times 2", spoken("line3 * 2"))
    }

    @Test
    fun `a factorial and a degree are words too`() {
        assertEquals("5 factorial", spoken("5!"))
        assertEquals("90 degrees", spoken("90°"))
    }

    private fun spoken(source: String): String {
        val parsed = Engine.parse(source, EvalContext(locale = Locale.US))
        val ast = (parsed as? CalcResult.Ok)?.value ?: error("`$source` did not parse: $parsed")
        return ExpressionSpeech.of(ast, words)
    }
}
