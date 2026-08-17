package app.lineo.ui.editor

import app.lineo.engine.CalcError
import app.lineo.registry.EditorCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Command application and the unfinished/wrong distinction.
 *
 * The engine is real here rather than faked: what these assert is how the editor *reacts*
 * to what the engine says, and a fake would let the reaction drift from the thing it
 * reacts to. The debounce is asserted separately, where a fake is the point.
 */
class EditorStateTest {

    @Test
    fun `text is inserted at the caret, not appended`() {
        val editor = EditorState()
        editor.setText("15", newCaret = 1)

        editor.apply(EditorCommand.InsertText("0"))

        assertEquals("105", editor.text)
        assertEquals(2, editor.caret)
    }

    @Test
    fun `a function lands with the caret between its brackets`() {
        val editor = EditorState()

        editor.apply(EditorCommand.InsertFunction(name = "sqrt", arity = 1))

        assertEquals("sqrt()", editor.text)
        assertEquals("sqrt(".length, editor.caret)
    }

    @Test
    fun `backspace at the start of the line does nothing rather than throwing`() {
        val editor = EditorState()
        editor.setText("5", newCaret = 0)

        editor.apply(EditorCommand.Backspace)

        assertEquals("5", editor.text)
        assertEquals(0, editor.caret)
    }

    @Test
    fun `all clear empties the line`() {
        val editor = EditorState()
        editor.setText("1 + 2")

        editor.apply(EditorCommand.ClearLine)

        assertEquals("", editor.text)
        assertEquals(0, editor.caret)
    }

    @Test
    fun `moving the caret past either end clamps instead of going out of bounds`() {
        val editor = EditorState()
        editor.setText("12")

        editor.apply(EditorCommand.MoveCursor(delta = 99))
        assertEquals(2, editor.caret)

        editor.apply(EditorCommand.MoveCursor(delta = -99))
        assertEquals(0, editor.caret)
    }

    @Test
    fun `toggling the text keyboard leaves the line alone`() {
        val editor = EditorState()
        editor.setText("1 + 2")

        editor.apply(EditorCommand.ToggleTextInput)

        assertEquals("1 + 2", editor.text)
    }

    @Test
    fun `an empty line evaluates to nothing`() {
        val editor = EditorState()

        assertEquals(EditorEvaluation.Empty, editor.evaluateNow("   "))
    }

    @Test
    fun `a trailing operator is unfinished, not an error`() {
        // The P0-14 definition of done, verbatim: typing 5 + shows no error state.
        val editor = EditorState()

        assertEquals(EditorEvaluation.Unfinished, editor.evaluateNow("5 +"))
    }

    @Test
    fun `an unclosed bracket is unfinished`() {
        val editor = EditorState()

        assertEquals(EditorEvaluation.Unfinished, editor.evaluateNow("(1 + 2"))
    }

    @Test
    fun `an unknown name is an error the moment it appears`() {
        // Typing more cannot make sni valid, so staying quiet would only delay the news.
        val editor = EditorState()

        val evaluation = editor.evaluateNow("sni(1)")

        assertTrue(evaluation.toString(), evaluation is EditorEvaluation.Failure)
        assertTrue((evaluation as EditorEvaluation.Failure).error is CalcError.UnknownIdentifier)
    }

    @Test
    fun `a complete line evaluates to a result`() {
        val editor = EditorState()

        val evaluation = editor.evaluateNow("0.1 + 0.2")

        assertTrue(evaluation.toString(), evaluation is EditorEvaluation.Result)
        assertEquals("0.3", (evaluation as EditorEvaluation.Result).value.canonicalString())
    }
}
