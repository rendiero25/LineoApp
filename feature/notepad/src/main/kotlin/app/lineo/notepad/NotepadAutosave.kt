package app.lineo.notepad

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Keeps a document on disk in step with the one being edited.
 *
 * **Back never discards work.** There is no save button and no "discard changes?" dialog:
 * the document is written as it is typed, so pressing back, being swiped away, or being
 * killed by the system all leave the same thing behind — what the user could see on screen.
 * A prompt would only exist to ask them to confirm the work they had already done.
 *
 * Writes are debounced, not skipped. A keystroke changes the document, and the debounce only
 * decides *when* the row is written; [flush] then writes whatever the last one left, which is
 * what a lifecycle stop calls so that the window between the final keystroke and the save
 * cannot outlive the screen.
 *
 * @param debounceMillis how long typing has to pause before a write. Long enough that a fast
 *   typist costs one write rather than one per character, short enough that it has passed
 *   before a user who typed something can reach the back gesture.
 */
class NotepadAutosave(
    private val store: NotepadStore,
    private val documentId: Long,
    private val state: NotepadState,
    private val debounceMillis: Long = DEBOUNCE_MILLIS,
) {

    /**
     * Writes the document whenever it settles. Runs until the caller's scope is cancelled.
     *
     * Driven from `uiState` because that is the one thing that changes on every edit; what is
     * *written* is `stored`, the id-bearing form, since display text names ordinals and would
     * repoint every reference the next time the document was read back.
     */
    @OptIn(FlowPreview::class)
    suspend fun run() {
        state.uiState
            .map { state.stored }
            .distinctUntilChanged()
            .debounce(debounceMillis)
            .collect { document -> store.save(documentId, document) }
    }

    /** Writes now, without waiting for the debounce. What a stop, or a back press, calls. */
    suspend fun flush() {
        store.save(documentId, state.stored)
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 500L
    }
}
