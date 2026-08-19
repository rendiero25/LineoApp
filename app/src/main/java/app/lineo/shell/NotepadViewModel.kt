package app.lineo.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lineo.di.ApplicationScope
import app.lineo.notepad.NotepadAutosave
import app.lineo.notepad.NotepadState
import app.lineo.notepad.NotepadStore
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
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val opened = MutableStateFlow<OpenedNotepad?>(null)

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
     * Opens the notepad, once. Later calls are ignored, so a recomposition cannot reload the
     * document out from under the caret.
     *
     */
    fun open(title: String) {
        if (opened.value != null || autosave != null) return
        autosave = viewModelScope.launch {
            val notepad = store.openOrCreate(title)
            val state = NotepadState(notepad.document)
            opened.value = OpenedNotepad(state, NotepadAutosave(store, notepad.id, state))
            opened.value?.autosave?.run()
        }
    }

    /**
     * Writes the document now.
     *
     * Called when the screen stops — which covers back, the recents switcher, and the home
     * gesture. **Not** on `viewModelScope`: a back press stops the screen and finishes the
     * activity in the same breath, and the scope is cancelled with it, so the write would be
     * cancelled by the very event that asked for it. It goes to a scope that outlives the
     * screen instead.
     */
    fun flush() {
        val notepad = opened.value ?: return
        applicationScope.launch { notepad.autosave.flush() }
    }
}

/** The open document: what the screen renders from, and what keeps it on disk. */
data class OpenedNotepad(
    val state: NotepadState,
    internal val autosave: NotepadAutosave,
)
