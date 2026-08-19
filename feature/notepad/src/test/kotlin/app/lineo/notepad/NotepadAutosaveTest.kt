package app.lineo.notepad

import app.lineo.registry.EditorCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Back never discards work.
 *
 * The guarantee is not "the user is asked before losing something", it is "there is nothing
 * to lose": the document is written as it is typed. These tests assert both halves of that —
 * that typing alone reaches the store, and that a stop writes whatever the debounce was still
 * holding.
 *
 * Time is virtual. Waiting half a second per assertion would make the suite slower than the
 * thing it measures, and a real delay proves nothing a test scheduler does not.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotepadAutosaveTest {

    private val repository = FakeDocumentRepository()
    private val store = NotepadStore(repository)

    @Test
    fun `typing is written once the typing stops`() = runTest {
        val id = repository.createDocument("notepad")
        val state = NotepadState(store.open(id).append())
        backgroundScope.launchAutosave(id, state)

        state.type("2 + 3")
        advanceTimeBy(DEBOUNCE + 1)

        assertEquals(listOf("2 + 3"), store.open(id).lines.map { it.source })
    }

    @Test
    fun `a burst of keystrokes costs one write`() = runTest {
        val id = repository.createDocument("notepad")
        val state = NotepadState(store.open(id).append())
        backgroundScope.launchAutosave(id, state)

        state.type("123456")
        advanceTimeBy(DEBOUNCE + 1)

        assertEquals(1, repository.writes.size)
    }

    @Test
    fun `a stop writes what the debounce was still holding`() = runTest {
        val id = repository.createDocument("notepad")
        val state = NotepadState(store.open(id).append())
        backgroundScope.launchAutosave(id, state)
        val autosave = NotepadAutosave(store, id, state)

        state.type("41 + 1")
        // Not a millisecond has passed: the debounce has not fired and would not have.
        autosave.flush()

        assertEquals(listOf("41 + 1"), store.open(id).lines.map { it.source })
    }

    @Test
    fun `what was typed is still there when the document is opened again`() = runTest {
        val id = repository.createDocument("notepad")
        val state = NotepadState(store.open(id).append())
        val autosave = NotepadAutosave(store, id, state)

        state.type("rate = 12500")
        state.apply(EditorCommand.NewLine)
        state.type("3 * rate")
        autosave.flush()

        // A new state built from a fresh read is what survives process death: nothing of the
        // first one is carried over, not even its line ids.
        val reopened = NotepadState(store.open(id))
        assertEquals(listOf("rate = 12500", "3 * rate"), reopened.uiState.value.lines.map { it.text })
        assertEquals("37500", reopened.value(2))
    }

    @Test
    fun `an untouched document is not written`() = runTest {
        val id = repository.createDocument("notepad")
        val state = NotepadState(store.open(id).append())
        store.save(id, state.stored)
        repository.writes.clear()
        backgroundScope.launchAutosave(id, state)

        advanceUntilIdle()

        assertEquals(emptyList<String>(), repository.writes.map { it.source })
    }

    private fun CoroutineScope.launchAutosave(id: Long, state: NotepadState) {
        launch { NotepadAutosave(store, id, state).run() }
    }

    private fun NotepadState.type(text: String) {
        text.forEach { character -> apply(EditorCommand.InsertText(character.toString())) }
    }

    private fun NotepadState.value(ordinal: Int): String? =
        (uiState.value.lines[ordinal - 1].evaluation as? LineEvaluation.Value)?.value?.canonicalString()

    private companion object {
        const val DEBOUNCE = 500L
    }
}
