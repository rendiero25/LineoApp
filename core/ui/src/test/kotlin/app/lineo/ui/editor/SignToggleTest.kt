package app.lineo.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The `±` key, which is the only key on the keypad whose behaviour depends on what is
 * already typed.
 *
 * The cases worth defending are the ones where a naive `InsertText("-")` gets it wrong:
 * pressing twice, and a line where `-` is already doing a different job.
 */
class SignToggleTest {

    @Test
    fun `a positive number becomes negative`() {
        assertEquals(SignToggle("-42", 3), toggle("42", caret = 2))
    }

    @Test
    fun `pressing twice leaves the line exactly as it was`() {
        val once = toggle("42", caret = 2)
        val twice = toggle(once.text, once.caret)

        assertEquals(SignToggle("42", 2), twice)
    }

    @Test
    fun `only the number at the caret is touched`() {
        assertEquals(SignToggle("100 + -42", 9), toggle("100 + 42", caret = 8))
    }

    @Test
    fun `a subtraction is not mistaken for a sign`() {
        // The minus in `100 - 42` belongs to the 100. Removing it would turn a subtraction
        // into `100 42`, which is a different expression and a silent one.
        assertEquals(SignToggle("100 - -42", 9), toggle("100 - 42", caret = 8))
    }

    @Test
    fun `a sign after an operator is removed rather than doubled`() {
        assertEquals(SignToggle("5 * 3", 5), toggle("5 * -3", caret = 6))
    }

    @Test
    fun `a decimal keeps its whole number`() {
        assertEquals(SignToggle("-3.14", 5), toggle("3.14", caret = 4))
    }

    @Test
    fun `a comma decimal separator is part of the number too`() {
        assertEquals(SignToggle("-3,14", 5), toggle("3,14", caret = 4, separator = ','))
    }

    @Test
    fun `an empty line starts a negative number`() {
        assertEquals(SignToggle("-", 1), toggle("", caret = 0))
    }

    @Test
    fun `after an operator with nothing typed yet, a minus is started`() {
        assertEquals(SignToggle("5 * -", 5), toggle("5 * ", caret = 4))
    }

    @Test
    fun `the caret inside a number still finds it`() {
        // Caret between the 1 and the 2 of 12: the run scanned back is "1".
        assertEquals(SignToggle("-12", 2), toggle("12", caret = 1))
    }

    private fun toggle(text: String, caret: Int, separator: Char = '.') =
        toggleSign(text, caret, separator)
}
