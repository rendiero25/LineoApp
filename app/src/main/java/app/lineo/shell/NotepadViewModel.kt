package app.lineo.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lineo.data.repository.HistoryRepository
import app.lineo.di.ApplicationScope
import app.lineo.engine.EvalContext
import app.lineo.notepad.NotepadAutosave
import app.lineo.notepad.NotepadEvaluator
import app.lineo.notepad.NotepadState
import app.lineo.notepad.NotepadStore
import app.lineo.registry.EditorCommand
import app.lineo.ui.format.QuantityFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds the open notepad for as long as the screen exists.
 *
 * A `ViewModel` and not a remembered state holder, because the document has to survive a
 * rotation, a fold, and a night-mode change without being read from disk again — and because
 * `docs/ANDROID_STANDARDS.md` §1 puts screen-level state here.
 *
 * No `Context`, no `Resources`: the document's title arrives from the composable that has a
 * `stringResource` to read it with, which is why [open] takes it as an argument instead of
 * this class knowing what a notepad is called.
 */
@HiltViewModel
class NotepadViewModel @Inject constructor(
    private val store: NotepadStore,
    private val history: HistoryRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val opened = MutableStateFlow<OpenedNotepad?>(null)

    /** Records finished lines. Built with the open document, since it needs the locale. */
    private var recorder: NotepadHistory? = null

    /** What the document was last read against, so an unchanged context costs nothing. */
    private var lastContext: EvalContext? = null

    /** What `±` treats as part of a number, kept beside the context it arrived with. */
    private var lastSeparator: Char = '.'

    /**
     * The screen, or `null` while the first read is in flight.
     *
     * Null rather than an empty document: an empty notepad is a real state — it is what the
     * user sees on the first run — and showing one for the length of a disk read would put a
     * caret in a line that is about to be replaced by their own.
     */
    val notepad: StateFlow<OpenedNotepad?> = opened.asStateFlow()

    private var autosave: Job? = null

    /**
     * Re-reads the open document under a changed context — a new angle mode, or the new
     * reading locale a separator setting resolves to.
     *
     * Called from the route whenever the context changes, which is how a settings change
     * reaches a notepad that is already open: `sin(30)` means something different in RAD, and
     * an answer left over from the old mode would be wrong on screen with nothing to say so.
     * An unchanged context costs nothing.
     *
     * @param display how results are written now. The tape is given the new formatter too, so
     *   a line finished after the change is recorded the way the user saw it.
     */
    fun apply(context: EvalContext, display: QuantityFormat, decimalSeparator: Char) {
        if (context == lastContext) return
        lastContext = context
        lastSeparator = decimalSeparator
        recorder?.format = display
        // May be null: the stored settings arrive from DataStore while the first read is still
        // in flight, and there is no document to re-read yet. Nothing is lost — [open] builds
        // the state from these fields, so the document is evaluated against what is stored
        // here rather than against the defaults the first composition passed in. Found on a
        // device: a comma-separator user's first line read as `Unexpected ','` until they
        // changed something.
        opened.value?.state?.evaluateWith(NotepadEvaluator(context), decimalSeparator)
    }

    /**
     * Opens the notepad, once. Later calls are ignored, so a recomposition cannot reload the
     * document out from under the caret.
     *
     * @param title what an unnamed document is called. Read where there is a `Context` to
     *   read it with, since this class has none (`docs/ANDROID_STANDARDS.md` §1).
     * @param context what every line is evaluated against: the locale, the angle mode, and the
     *   functions and units the build offers, module contributions included. Passed in for the
     *   same reason as the title — all of it is resolved from settings the caller holds, and
     *   without it the notepad would evaluate against the built-ins in the platform's locale.
     * @param display how a result is written: the formatter the screen renders with, so the
     *   tape records the digits the user actually saw.
     * @param decimalSeparator what `±` treats as part of a number, from the same settings the
     *   keypad reads (`docs/CONVENTIONS.md` §2).
     */
    fun open(title: String, context: EvalContext, display: QuantityFormat, decimalSeparator: Char) {
        if (opened.value != null || autosave != null) return
        lastContext = context
        lastSeparator = decimalSeparator
        val tape = NotepadHistory(history, display)
        recorder = tape
        autosave = viewModelScope.launch {
            val notepad = store.openOrCreate(title)
            // The newest settings, not the ones this call carried: the first composition passes
            // the defaults, and the stored settings arrive from DataStore a moment later —
            // usually while this read is still in flight.
            val state = NotepadState(
                document = notepad.document,
                evaluator = NotepadEvaluator(lastContext ?: context),
                decimalSeparator = lastSeparator,
            )
            opened.value = OpenedNotepad(state, NotepadAutosave(store, notepad.id, state))
            // The tape runs beside the autosave rather than inside it: one writes the document
            // the user is still editing, the other records the lines they have finished with,
            // and neither should stop because the other did.
            launch { tape.run(state.uiState) }
            opened.value?.autosave?.run()
        }
    }

    /**
     * Puts a history entry back into the document, as a new line.
     *
     * The expression and not the result: the expression is what can be recalculated and
     * edited, which is what "reuse" means. It goes on a line of its own rather than into the
     * line the caret is in — that line is the user's, and half-typed work is not something to
     * overwrite. A blank line is written into rather than pushed down, since there is nothing
     * there to keep.
     */
    fun reuse(expression: String) {
        val state = opened.value?.state ?: return
        val focused = state.uiState.value.focusedLine
        if (focused == null) return
        if (focused.text.isNotBlank()) state.apply(EditorCommand.NewLine)
        state.setText(expression)
    }

    /**
     * Writes the document now, and puts the line the caret is still in on the tape.
     *
     * Called when the screen stops — which covers back, the recents switcher, and the home
     * gesture. **Not** on `viewModelScope`: a back press stops the screen and finishes the
     * activity in the same breath, and the scope is cancelled with it, so the write would be
     * cancelled by the very event that asked for it. It goes to a scope that outlives the
     * screen instead.
     */
    fun flush() {
        val notepad = opened.value ?: return
        val tape = recorder
        applicationScope.launch {
            notepad.autosave.flush()
            // The line the caret is still in has not been left, and the screen is going away:
            // this is the last moment it can reach the tape.
            tape?.flush(notepad.state.uiState.value)
        }
    }
}

/** The open document: what the screen renders from, and what keeps it on disk. */
data class OpenedNotepad(
    val state: NotepadState,
    internal val autosave: NotepadAutosave,
)
