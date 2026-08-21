package app.lineo.shell

import app.lineo.data.repository.HistoryRepository
import app.lineo.engine.LineId
import app.lineo.notepad.LineEvaluation
import app.lineo.notepad.NotepadUiState
import app.lineo.ui.format.QuantityFormat
import kotlinx.coroutines.flow.Flow

/**
 * Puts finished notepad lines on the history tape (P1-06).
 *
 * **A line is finished when the caret leaves it.** There is no "commit" in a notepad — a
 * line is evaluated as it is typed — so the only honest moment to record one is when the
 * user moves on: pressing `=`, tapping another line, or closing the screen. Recording on
 * every evaluation would put `1`, `1+`, `1+2` on the tape and leave 50 entries of one
 * calculation; recording only on `=` would miss the line a user typed and then tapped away
 * from, which is the same act.
 *
 * The same line is never recorded twice in a row. Coming back to a line, editing it and
 * leaving again records the new text — it is a different calculation — but leaving a line
 * untouched does not.
 *
 * What is stored is the source and the **formatted** result, because history is a record of
 * what the user saw (`HistoryEntry`). Re-evaluating it later under another angle mode or
 * locale would rewrite the past.
 */
internal class NotepadHistory(
    private val repository: HistoryRepository,
    format: QuantityFormat,
) {

    /**
     * How a result is written on the tape — the same formatter the screen renders with.
     *
     * A `var`, because the settings can change while the notepad is open. What is already on
     * the tape is left alone: it says what the user saw at the time, and that does not change
     * because a setting did.
     */
    var format: QuantityFormat = format

    private var lastRecorded: Pair<LineId, String>? = null
    private var previouslyFocused: LineId? = null

    /**
     * Watches [states] and records each line the caret leaves.
     *
     * Suspends for as long as the notepad is open. The document is not consulted: everything
     * needed is in the state the screen is already rendering from.
     */
    suspend fun run(states: Flow<NotepadUiState>) {
        states.collect { state -> onState(state) }
    }

    /** Records the line the caret has just left, if it had a value. Public for the flush path. */
    suspend fun onState(state: NotepadUiState) {
        val left = previouslyFocused
        previouslyFocused = state.focused
        if (left == null || left == state.focused) return
        record(state, left)
    }

    /** Records whatever the caret is in now — the last chance, when the screen is going away. */
    suspend fun flush(state: NotepadUiState) {
        record(state, state.focused ?: return)
    }

    private suspend fun record(state: NotepadUiState, id: LineId) {
        val line = state.lines.firstOrNull { it.id == id } ?: return
        val value = (line.evaluation as? LineEvaluation.Value)?.value ?: return
        val expression = line.text.trim()
        if (expression.isEmpty()) return
        if (lastRecorded == id to expression) return
        lastRecorded = id to expression
        repository.record(expression = expression, resultText = format.format(value).text)
    }
}
