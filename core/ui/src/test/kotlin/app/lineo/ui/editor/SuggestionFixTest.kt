package app.lineo.ui.editor

import app.lineo.engine.CalcError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tap-to-fix chip: what it is offered for, and what tapping it does.
 *
 * The P0-14 definition of done names `sni(1)`, so that is the case the first test uses
 * verbatim — through the real engine, so the suggestion is the one a user would actually
 * be shown rather than one this test invented.
 */
class SuggestionFixTest {

    @Test
    fun `a misspelled function is offered its nearest match`() {
        val editor = EditorState()

        val evaluation = editor.evaluateNow("sni(1)")

        val error = (evaluation as EditorEvaluation.Failure).error as CalcError.UnknownIdentifier
        assertEquals("sni", error.name)
        assertEquals("sin", error.suggestion)
    }

    @Test
    fun `tapping the chip replaces the name and leaves the rest of the line alone`() {
        val editor = EditorState()
        editor.setText("2 * sni(1) + 3")
        val error = (editor.evaluateNow() as EditorEvaluation.Failure).error as CalcError.UnknownIdentifier

        editor.applySuggestion(error)

        assertEquals("2 * sin(1) + 3", editor.text)
    }

    @Test
    fun `the caret lands after the replacement, ready to keep typing`() {
        val editor = EditorState()
        editor.setText("sni(1)")
        val error = (editor.evaluateNow() as EditorEvaluation.Failure).error as CalcError.UnknownIdentifier

        editor.applySuggestion(error)

        assertEquals("sin".length, editor.caret)
    }

    @Test
    fun `the fixed line then evaluates`() {
        val editor = EditorState()
        editor.setText("sni(0)")
        val error = (editor.evaluateNow() as EditorEvaluation.Failure).error as CalcError.UnknownIdentifier

        editor.applySuggestion(error)

        assertTrue(editor.evaluateNow().toString(), editor.evaluateNow() is EditorEvaluation.Result)
    }

    @Test
    fun `an unknown name with no near match changes nothing when applied`() {
        // The engine offers no suggestion for something unlike every known name, and the
        // editor must not invent one — applySuggestion is a no-op rather than a deletion.
        val editor = EditorState()
        editor.setText("zzzqqq + 1")
        val error = (editor.evaluateNow() as EditorEvaluation.Failure).error as CalcError.UnknownIdentifier

        editor.applySuggestion(error)

        assertEquals("zzzqqq + 1", editor.text)
    }
}
