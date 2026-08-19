package app.lineo.notepad

import app.lineo.engine.CalcError
import app.lineo.registry.EditorCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the chip row is offered, and what the fix chip does.
 *
 * Both are state, not rendering, so both are asserted here rather than in a snapshot: a
 * picture can show that a chip exists, but not that it offers the name the parser will
 * actually read at that point in the document.
 */
class NotepadSuggestionsTest {

    @Test
    fun `a variable defined above the caret is offered`() {
        val state = NotepadState(Documents.of("rate = 12500", "3 * 4"))
        state.focus(state.idAt(2))

        assertEquals(listOf("rate"), state.variableSuggestions())
    }

    @Test
    fun `a variable defined below the caret is not offered`() {
        val state = NotepadState(Documents.of("3 * 4", "rate = 12500"))
        state.focus(state.idAt(1))

        assertEquals(emptyList<String>(), state.variableSuggestions())
    }

    @Test
    fun `the nearest definition comes first`() {
        val state = NotepadState(Documents.of("first = 1", "second = 2", "3"))
        state.focus(state.idAt(3))

        assertEquals(listOf("second", "first"), state.variableSuggestions())
    }

    @Test
    fun `a unit a line above produced is offered, and a dimensionless line adds nothing`() {
        val state = NotepadState(Documents.of("5 km + 300 m", "2 + 2", "0"))
        state.focus(state.idAt(3))

        assertEquals(listOf("km"), state.unitSuggestions())
    }

    @Test
    fun `the same unit twice is offered once`() {
        val state = NotepadState(Documents.of("1 km", "2 km", "0"))
        state.focus(state.idAt(3))

        assertEquals(listOf("km"), state.unitSuggestions())
    }

    @Test
    fun `nothing is offered when nothing has focus`() {
        val state = NotepadState(Documents.of("rate = 1", "2"))
        state.focus(state.idAt(1))

        assertEquals(emptyList<NotepadSuggestion>(), state.uiState.value.suggestions)
    }

    @Test
    fun `the fix chip replaces the name the engine did not recognise`() {
        val state = NotepadState(Documents.of("sni(1)"))
        val line = state.uiState.value.lines.single()
        val error = (line.evaluation as LineEvaluation.Failed).error as CalcError.UnknownIdentifier

        state.applySuggestion(line.id, error)

        assertEquals("sin(1)", state.uiState.value.lines.single().text)
    }

    @Test
    fun `fixing a line that is not focused focuses it first`() {
        val state = NotepadState(Documents.of("1 + 1", "sni(1)"))
        val second = state.uiState.value.lines[1]
        val error = (second.evaluation as LineEvaluation.Failed).error as CalcError.UnknownIdentifier

        state.applySuggestion(second.id, error)

        assertEquals(second.id, state.uiState.value.focused)
        assertEquals("sin(1)", state.uiState.value.lines[1].text)
    }

    @Test
    fun `a chip inserts its text through the same command path as a key`() {
        val state = NotepadState(Documents.of("rate = 2", ""))
        state.focus(state.idAt(2))

        state.apply(EditorCommand.InsertText("rate"))

        assertEquals("rate", state.uiState.value.lines[1].text)
        assertTrue(state.uiState.value.lines[1].evaluation is LineEvaluation.Value)
    }

    @Test
    fun `text arriving with newlines becomes one line each`() {
        val state = NotepadState()

        state.setText("2 + 2\n3 * 3\n")

        assertEquals(listOf("2 + 2", "3 * 3", ""), state.uiState.value.lines.map { it.text })
        assertEquals(state.uiState.value.lines.last().id, state.uiState.value.focused)
        assertEquals("9", (state.uiState.value.lines[1].evaluation as LineEvaluation.Value).value.canonicalString())
    }

    private fun NotepadState.idAt(ordinal: Int) = uiState.value.lines[ordinal - 1].id

    private fun NotepadState.variableSuggestions(): List<String> = uiState.value.suggestions
        .filter { it.kind == NotepadSuggestion.Kind.Variable }
        .map { it.text }

    private fun NotepadState.unitSuggestions(): List<String> = uiState.value.suggestions
        .filter { it.kind == NotepadSuggestion.Kind.Unit }
        .map { it.text }
}
