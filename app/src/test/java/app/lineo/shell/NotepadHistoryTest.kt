package app.lineo.shell

import app.lineo.data.model.HistoryEntry
import app.lineo.data.repository.FREE_TIER_HISTORY_LIMIT
import app.lineo.data.repository.HistoryRepository
import app.lineo.engine.EvalContext
import app.lineo.notepad.NotepadDocument
import app.lineo.notepad.NotepadEvaluator
import app.lineo.notepad.NotepadState
import app.lineo.registry.EditorCommand
import app.lineo.ui.format.QuantityFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.Locale

/**
 * What reaches the tape, and what does not (P1-06).
 *
 * The rule under test is "a line is finished when the caret leaves it", which is the only
 * moment a notepad offers: there is no commit, and every keystroke is evaluated. The tests
 * therefore drive a real `NotepadState` and move focus, rather than calling the recorder with
 * states invented by hand — the states a screen actually produces are the point.
 */
class NotepadHistoryTest {

    private val repository = RecordingHistoryRepository()
    private val recorder = NotepadHistory(repository, QuantityFormat(Locale.US))

    private fun notepad(vararg lines: String): NotepadState {
        var document = NotepadDocument()
        lines.forEach { document = document.append(it) }
        if (lines.isEmpty()) document = document.append()
        return NotepadState(document = document, evaluator = NotepadEvaluator(EvalContext(locale = Locale.US)))
    }

    private suspend fun NotepadState.settle() = recorder.onState(uiState.value)

    @Test
    fun `a line is recorded when the caret leaves it`() = runTest {
        val state = notepad("2 + 3", "")
        val first = state.uiState.value.lines[0].id
        val second = state.uiState.value.lines[1].id

        state.focus(first)
        state.settle()
        state.focus(second)
        state.settle()

        assertEquals(listOf("2 + 3" to "5"), repository.recorded)
    }

    @Test
    fun `nothing is recorded while a line is still being typed`() = runTest {
        val state = notepad("")
        state.focus(state.uiState.value.lines[0].id)
        "12+3".forEach {
            state.apply(EditorCommand.InsertText(it.toString()))
            state.settle()
        }

        // 1, 12, 12+, 12+3 all evaluated; none of them is a finished calculation.
        assertEquals(emptyList<Pair<String, String>>(), repository.recorded)
    }

    @Test
    fun `a line with no value is not recorded`() = runTest {
        val state = notepad("1 km + 3 kg", "")

        state.focus(state.uiState.value.lines[0].id)
        state.settle()
        state.focus(state.uiState.value.lines[1].id)
        state.settle()

        assertEquals(emptyList<Pair<String, String>>(), repository.recorded)
    }

    @Test
    fun `leaving a line twice without editing it records it once`() = runTest {
        val state = notepad("6 * 7", "")
        val first = state.uiState.value.lines[0].id
        val second = state.uiState.value.lines[1].id

        repeat(2) {
            state.focus(first)
            state.settle()
            state.focus(second)
            state.settle()
        }

        assertEquals(listOf("6 * 7" to "42"), repository.recorded)
    }

    @Test
    fun `editing a line and leaving again records the new calculation`() = runTest {
        val state = notepad("6 * 7", "")
        val first = state.uiState.value.lines[0].id
        val second = state.uiState.value.lines[1].id

        state.focus(first)
        state.settle()
        state.focus(second)
        state.settle()
        state.focus(first)
        state.settle()
        state.apply(EditorCommand.InsertText("0"))
        state.settle()
        state.focus(second)
        state.settle()

        assertEquals(listOf("6 * 7" to "42", "6 * 70" to "420"), repository.recorded)
    }

    @Test
    fun `the result is formatted the way the user saw it`() = runTest {
        // Not `canonicalString`: history is a record of what was on screen, so it carries the
        // grouping of the locale the line was read in.
        val state = notepad("1234567 + 1", "")
        state.focus(state.uiState.value.lines[0].id)
        state.settle()
        state.focus(state.uiState.value.lines[1].id)
        state.settle()

        assertEquals(listOf("1234567 + 1" to "1,234,568"), repository.recorded)
    }

    @Test
    fun `the flush records the line the caret is still in`() = runTest {
        // Back, home and the recents switcher all reach `flush`, and the line being typed has
        // not been left by then — this is its last chance at the tape.
        val state = notepad("8 * 8")
        state.focus(state.uiState.value.lines[0].id)

        recorder.flush(state.uiState.value)

        assertEquals(listOf("8 * 8" to "64"), repository.recorded)
    }
}

/** Records what it was told, in order. The cap is the repository's and is tested in `:core:data`. */
internal class RecordingHistoryRepository : HistoryRepository {

    val recorded = mutableListOf<Pair<String, String>>()
    private val entries = MutableStateFlow<List<HistoryEntry>>(emptyList())

    override fun getEntriesStream(limit: Int): Flow<List<HistoryEntry>> = entries.map { it.take(limit) }

    override suspend fun record(expression: String, resultText: String, moduleId: String?, limit: Int) {
        recorded += expression to resultText
        entries.value = listOf(
            HistoryEntry(
                id = recorded.size.toLong(),
                expression = expression,
                resultText = resultText,
                createdAt = Instant.EPOCH,
                moduleId = moduleId,
            ),
        ) + entries.value.take(FREE_TIER_HISTORY_LIMIT - 1)
    }

    override suspend fun clear() {
        recorded.clear()
        entries.value = emptyList()
    }
}
