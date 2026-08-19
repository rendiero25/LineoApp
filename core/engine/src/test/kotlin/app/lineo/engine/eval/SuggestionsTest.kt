package app.lineo.engine.eval

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * What may be offered as a fix, and what may not.
 *
 * The chip this feeds is a tap away from rewriting what the user typed, so a wrong guess
 * costs more than no guess. The candidate list is the real one in shape — variables,
 * constants, functions and every unit symbol — because the unit symbols are what made a
 * one-letter name match half the registry.
 */
class SuggestionsTest {

    private val candidates = listOf(
        "sin", "cos", "tan", "sinh", "log", "ln", "sqrt", "round",
        "pi", "e", "phi",
        "m", "s", "g", "K", "N", "km", "kg", "mm", "cm",
    )

    @Test
    fun `a single letter is not guessed about`() {
        assertNull(Suggestions.nearest("x", candidates))
    }

    @Test
    fun `a single letter does not collect the nearest unit symbol`() {
        // The bug this test exists for: at two edits, `x` matched `K`, `N`, `m`, `s` and the
        // rest, and the tie-break handed over whichever sorted first.
        assertNull(Suggestions.nearest("q", candidates))
    }

    @Test
    fun `two letters allow one edit, not two`() {
        assertEquals("pi", Suggestions.nearest("pj", candidates))
        assertNull(Suggestions.nearest("qq", candidates))
    }

    @Test
    fun `a swap is one edit`() {
        assertEquals("sin", Suggestions.nearest("sni", candidates))
        assertEquals("cos", Suggestions.nearest("cso", candidates))
    }

    @Test
    fun `the nearest in length wins a tie on distance`() {
        // `sinh` is one insertion from `sin`, and so is the typo — length breaks the tie.
        assertEquals("sin", Suggestions.nearest("sim", candidates))
    }

    @Test
    fun `a longer name keeps the full tolerance`() {
        assertEquals("round", Suggestions.nearest("rouud", candidates))
        assertEquals("sqrt", Suggestions.nearest("sqroot", candidates))
    }

    @Test
    fun `case does not decide`() {
        // `K` is a candidate one edit from `k`, and so is `e`. Neither may win on sorting:
        // with two letters typed the answer is the one that is actually nearer.
        assertEquals("ln", Suggestions.nearest("lN", candidates))
    }

    @Test
    fun `the name itself is never suggested`() {
        assertNull(Suggestions.nearest("sin", listOf("sin")))
    }
}
