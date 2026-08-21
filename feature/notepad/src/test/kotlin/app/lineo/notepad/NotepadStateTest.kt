package app.lineo.notepad

import app.lineo.engine.parser.Ast
import app.lineo.engine.parser.BinaryOperator
import app.lineo.registry.EditorCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The screen's model: typing, splitting, joining, and which keyboard is up.
 *
 * Everything is asserted through `uiState`, because that is the whole of what a screen can
 * see (`docs/ANDROID_STANDARDS.md` §1 — no second channel). Where a test cares about what
 * would be *stored* rather than shown, it reads `stored`, which is the other half of the
 * display-versus-id boundary and the half that has to survive being written to disk.
 */
class NotepadStateTest {

    @Test
    fun `typing on the focused line evaluates it`() {
        val state = NotepadState()

        state.type("2 + 3")

        assertEquals("2 + 3", state.line(1).text)
        assertEquals("5", state.value(1))
    }

    @Test
    fun `a keypad press and a keyboard keystroke reach the document the same way`() {
        val state = NotepadState()

        state.apply(EditorCommand.InsertText("4"))
        state.apply(EditorCommand.InsertText("2"))

        assertEquals("42", state.line(1).text)
        assertEquals(2, state.uiState.value.caret)
    }

    @Test
    fun `the sign key behaves as it does in the single-line editor`() {
        // Not a second implementation: this is the assertion that it is the same one.
        val state = NotepadState()
        state.type("5 * 3")

        state.apply(EditorCommand.ToggleSign)

        assertEquals("5 * -3", state.line(1).text)
        assertEquals("-15", state.value(1))
    }

    @Test
    fun `a new line splits at the caret and takes the rest with it`() {
        val state = NotepadState()
        state.type("12 + 34")
        state.moveCaretTo(2)

        state.apply(EditorCommand.NewLine)

        assertEquals(listOf("12", " + 34"), state.uiState.value.lines.map { it.text })
        assertEquals(state.uiState.value.lines[1].id, state.uiState.value.focused)
        assertEquals(0, state.uiState.value.caret)
    }

    @Test
    fun `a new line at the very start pushes a blank line above and keeps the text where it is`() {
        val state = NotepadState()
        state.type("99")
        val line = state.uiState.value.lines.single().id
        state.moveCaretTo(0)

        state.apply(EditorCommand.NewLine)

        assertEquals(listOf("", "99"), state.uiState.value.lines.map { it.text })
        // Same id, second ordinal: the content did not move to a new line, the blank did.
        assertEquals(line, state.uiState.value.lines[1].id)
        assertEquals(line, state.uiState.value.focused)
    }

    @Test
    fun `backspace at the start of a line joins it to the one above`() {
        val state = NotepadState()
        state.type("12")
        state.apply(EditorCommand.NewLine)
        state.type("34")
        state.moveCaretTo(0)

        state.apply(EditorCommand.Backspace)

        assertEquals(listOf("1234"), state.uiState.value.lines.map { it.text })
        assertEquals(2, state.uiState.value.caret)
        assertEquals("1234", state.value(1))
    }

    @Test
    fun `backspace at the start of the first line does nothing`() {
        val state = NotepadState()
        state.type("7")
        state.moveCaretTo(0)

        state.apply(EditorCommand.Backspace)

        assertEquals(listOf("7"), state.uiState.value.lines.map { it.text })
    }

    @Test
    fun `a line that reads another keeps reading it when a line is inserted above`() {
        val state = NotepadState()
        state.type("100")
        state.apply(EditorCommand.NewLine)
        state.type("line1 * 2")
        val reader = state.uiState.value.lines[1].id

        // Split line 1, which pushes the referenced line down to ordinal 2.
        state.focus(state.uiState.value.lines[0].id, caret = 0)
        state.apply(EditorCommand.NewLine)

        assertEquals("200", state.valueOf(reader))
        assertEquals("line2 * 2", state.lineOf(reader).text)
    }

    @Test
    fun `what is stored names ids, whatever the screen shows`() {
        val state = NotepadState()
        state.type("50")
        state.apply(EditorCommand.NewLine)
        state.type("line1 + 1")

        val stored = state.stored.lines.last().source

        assertEquals("line${state.stored.lines.first().id.value} + 1", stored)
        assertEquals("line1 + 1", state.line(2).text)
    }

    @Test
    fun `typing a reference digit by digit does not rewrite itself under the user`() {
        // The reason a draft exists. `line12` passes through `line1`, which binds to a real
        // line; re-deriving the text from that binding would put the wrong digits on screen.
        val state = NotepadState()
        state.type("11")
        state.apply(EditorCommand.NewLine)
        state.type("22")
        state.apply(EditorCommand.NewLine)

        state.type("line1")
        assertEquals("line1", state.line(3).text)
        state.type("2", clear = false)

        assertEquals("line12", state.line(3).text)
    }

    @Test
    fun `focus on a line of words raises the keyboard, focus on a line of numbers does not`() {
        val state = NotepadState()
        state.type("price = 2")
        state.apply(EditorCommand.NewLine)
        state.type("40")
        val words = state.uiState.value.lines[0].id
        val numbers = state.uiState.value.lines[1].id

        state.focus(words)
        assertTrue(state.uiState.value.textInputActive)

        state.focus(numbers)
        assertFalse(state.uiState.value.textInputActive)
    }

    @Test
    fun `a deliberate surface switch is not undone by the next keystroke`() {
        val state = NotepadState()

        state.apply(EditorCommand.ToggleTextInput)
        state.apply(EditorCommand.InsertText("4"))

        assertTrue(state.uiState.value.textInputActive)
    }

    @Test
    fun `moving focus keeps the caret where it was asked to go`() {
        val state = NotepadState()
        state.type("1234")
        state.apply(EditorCommand.NewLine)
        state.type("5")
        val first = state.uiState.value.lines[0].id

        state.focus(first, caret = 2)

        assertEquals(first, state.uiState.value.focused)
        assertEquals(2, state.uiState.value.caret)
    }

    @Test
    fun `a variable defined above is visible below, and updates when it changes`() {
        val state = NotepadState()
        state.type("price = 100")
        state.apply(EditorCommand.NewLine)
        state.type("price * 2")
        val total = state.uiState.value.lines[1].id
        assertEquals("200", state.valueOf(total))

        state.focus(state.uiState.value.lines[0].id)
        state.type("price = 250", clear = true)

        assertEquals("500", state.valueOf(total))
    }

    @Test
    fun `a line that reads a failed line is shown as blocked, not as an error`() {
        val state = NotepadState()
        state.type("x = 1 km + 1 kg")
        state.apply(EditorCommand.NewLine)
        state.type("x * 2")

        val blocked = state.uiState.value.lines[1].evaluation

        assertTrue(blocked.toString(), blocked is LineEvaluation.Blocked)
    }

    @Test
    fun `a line carries the tree the screen reader speaks it from`() {
        val state = NotepadState()

        state.type("2^3")

        // `docs/CONVENTIONS.md` §8: what TalkBack says comes from the tree, so the tree has
        // to reach the screen. It is the parse the evaluation already did, not a second one.
        val ast = state.line(1).ast
        assertEquals(BinaryOperator.POWER, (ast as? Ast.Binary)?.operator)
    }

    @Test
    fun `a line that does not parse has no tree to speak`() {
        val state = NotepadState()

        state.type("2 +")

        assertNull(state.line(1).ast)
    }

    @Test
    fun `an arrow key walks the document a line at a time`() {
        val state = NotepadState()
        state.type("111")
        state.apply(EditorCommand.NewLine)
        state.type("222")
        state.apply(EditorCommand.NewLine)
        state.type("333")

        assertTrue(state.moveFocusBy(-1))
        assertEquals(state.uiState.value.lines[1].id, state.uiState.value.focused)

        assertTrue(state.moveFocusBy(1))
        assertEquals(state.uiState.value.lines[2].id, state.uiState.value.focused)
    }

    @Test
    fun `the column is kept where the new line is long enough for it`() {
        val state = NotepadState()
        state.type("123456")
        state.apply(EditorCommand.NewLine)
        state.type("99")
        state.moveCaretTo(1)

        // Up into a longer line: the caret stays in column 1 rather than jumping to the end.
        state.moveFocusBy(-1)

        assertEquals(1, state.uiState.value.caret)
    }

    @Test
    fun `the column is clamped where the new line is shorter`() {
        val state = NotepadState()
        state.type("12")
        state.apply(EditorCommand.NewLine)
        state.type("123456")

        state.moveFocusBy(-1)

        assertEquals(2, state.uiState.value.caret)
    }

    @Test
    fun `an arrow at the edge of the document is not ours to swallow`() {
        // False hands the key back to the platform, which is what every other field does at
        // the first line and the last.
        val state = NotepadState()
        state.type("1")

        assertFalse(state.moveFocusBy(-1))
        assertFalse(state.moveFocusBy(1))
    }

    /** Types [text] into the focused line, clearing it first unless told otherwise. */
    private fun NotepadState.type(text: String, clear: Boolean = true) {
        if (clear) apply(EditorCommand.ClearLine)
        text.forEach { character -> apply(EditorCommand.InsertText(character.toString())) }
    }

    private fun NotepadState.moveCaretTo(position: Int) {
        apply(EditorCommand.MoveCursor(position - uiState.value.caret))
    }

    private fun NotepadState.line(ordinal: Int): NotepadLineUiState = uiState.value.lines[ordinal - 1]

    private fun NotepadState.lineOf(id: app.lineo.engine.LineId): NotepadLineUiState =
        uiState.value.lines.single { it.id == id }

    private fun NotepadState.value(ordinal: Int): String? =
        (line(ordinal).evaluation as? LineEvaluation.Value)?.value?.canonicalString()

    private fun NotepadState.valueOf(id: app.lineo.engine.LineId): String? =
        (lineOf(id).evaluation as? LineEvaluation.Value)?.value?.canonicalString()
}
