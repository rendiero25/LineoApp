package app.lineo.notepad

import app.lineo.engine.LineId
import app.lineo.registry.EditorCommand
import app.lineo.ui.editor.EditedLine
import app.lineo.ui.editor.applying
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The notepad screen's model: a document, where the caret is, which surface is up, and what
 * every line currently evaluates to.
 *
 * Every input surface reaches it the same way — through [apply] with an `EditorCommand` —
 * so the keypad, the system keyboard and a suggestion chip cannot each edit the document
 * their own way. The line edit itself is `:core:ui`'s, the same code the single-line editor
 * runs, so `±` behaves identically in both.
 *
 * Not a `ViewModel`: `docs/ANDROID_STANDARDS.md` §1 keeps those at screen level, and this is
 * what a screen-level one would hold. It exposes [uiState] and nothing else, because a
 * second channel is a second thing to keep in step (§1 again — no one-shot events).
 *
 * @param document the document to open.
 * @param evaluator holds the parse cache, so give it the same lifetime as the open document.
 * @param decimalSeparator what `±` treats as part of a number (`docs/CONVENTIONS.md` §2).
 */
class NotepadState(
    document: NotepadDocument = NotepadDocument().append(),
    private val evaluator: NotepadEvaluator = NotepadEvaluator(),
    private val decimalSeparator: Char = '.',
) {

    private var document: NotepadDocument = document
    private var evaluation: DocumentEvaluation = evaluator.evaluate(document)
    private var focused: LineId? = document.lines.firstOrNull()?.id
    private var caret: Int = 0
    private var textInputActive: Boolean = false

    /**
     * The focused line as the user is typing it, in display form.
     *
     * Held apart from the document because a reference is bound the moment it is written:
     * typing `line12` passes through `line1`, and re-deriving the display text from what
     * that bound to would rewrite the digits under the user's fingers. While a line has
     * focus, what they typed is what they see.
     */
    private var draft: String = document.lines.firstOrNull()?.let { document.displayTextOf(it.id) }.orEmpty()

    private val state = MutableStateFlow(snapshot())

    /** The screen. */
    val uiState: StateFlow<NotepadUiState> = state.asStateFlow()

    /** The document as stored — ids in the references, ready to persist. */
    val stored: NotepadDocument get() = document

    /**
     * Moves focus, and picks the surface that line is written in.
     *
     * The context switch of `docs/ARCHITECTURE.md` §5: a line that starts with a letter is
     * being written in words, so the system keyboard comes up; anything else gets the
     * keypad. It fires on focus, not on every keystroke, so pressing `ABC` deliberately is
     * never undone by the next character typed.
     */
    fun focus(id: LineId, caret: Int? = null) {
        if (document.line(id) == null) return
        focused = id
        draft = displayTextOf(id)
        this.caret = (caret ?: draft.length).coerceIn(0, draft.length)
        textInputActive = prefersTextInput(draft)
        publish()
    }

    /**
     * Applies one command to the focused line.
     *
     * Three of them are not line edits and are handled here: `NewLine` splits the line,
     * `Backspace` at the very start joins it to the one above, and `ToggleTextInput` swaps
     * the surface. Everything else is `:core:ui`'s [applying].
     */
    fun apply(command: EditorCommand) {
        val id = focused ?: return
        when {
            command == EditorCommand.ToggleTextInput -> {
                textInputActive = !textInputActive
                publish()
            }

            command == EditorCommand.NewLine -> splitLine(id)
            command == EditorCommand.Backspace && caret == 0 -> joinWithPrevious(id)

            else -> {
                val edited = EditedLine(draft, caret).applying(command, decimalSeparator)
                write(id, edited.text, edited.caret)
            }
        }
    }

    /** Replaces the focused line wholesale — the system keyboard typing into the field. */
    fun setText(text: String, caret: Int = text.length) {
        val id = focused ?: return
        write(id, text, caret)
    }

    /**
     * Splits the focused line at the caret.
     *
     * What `=` and Enter do in a notepad. The text before the caret stays; the text after it
     * becomes a new line below, which takes focus — so pressing Enter in the middle of a
     * line never loses what was to the right of it.
     */
    private fun splitLine(id: LineId) {
        val index = document.lines.indexOfFirst { it.id == id }
        if (index < 0) return
        // At the very start of a line with something on it, the blank line goes *above* and
        // this line keeps both its text and its id. Identity follows the content, so a
        // reference to this line still reads what the user can see it pointing at — the
        // guarantee P1-01 exists for. Splitting anywhere else divides the text as typed.
        if (caret == 0 && draft.isNotEmpty()) {
            document = document.insertAt(index, "")
            evaluate()
            return
        }
        val before = draft.take(caret)
        val after = draft.drop(caret)

        write(id, before, before.length)
        document = document.insertAt(index + 1, after.let { canonical(it) })
        val created = document.lines[index + 1].id
        focused = created
        draft = after
        caret = 0
        // A new line inherits nothing: the surface follows what is on it, which is usually
        // nothing at all, and an empty line keeps whichever surface the user was using.
        if (after.isNotEmpty()) textInputActive = prefersTextInput(after)
        evaluate()
    }

    /**
     * Joins the focused line to the one above it — backspace at the start of a line.
     *
     * The line below is appended to the line above and disappears; the caret lands where the
     * join is, so a second backspace deletes the character before it, as in any editor. On
     * the first line there is nothing above, so nothing happens.
     */
    private fun joinWithPrevious(id: LineId) {
        val index = document.lines.indexOfFirst { it.id == id }
        if (index <= 0) return
        val previous = document.lines[index - 1].id
        val head = displayTextOf(previous)
        val tail = draft

        document = document.delete(id)
        focused = previous
        write(previous, head + tail, head.length)
    }

    /** Writes display text to a line, keeping the draft and the document in step. */
    private fun write(id: LineId, text: String, caret: Int) {
        draft = text
        this.caret = caret.coerceIn(0, text.length)
        document = document.setDisplayText(id, text)
        evaluate()
    }

    private fun canonical(displayText: String): String =
        LineReferences.toCanonical(displayText, document::idAtOrdinal)

    private fun evaluate() {
        evaluation = evaluator.reevaluate(evaluation, document)
        publish()
    }

    private fun publish() {
        state.value = snapshot()
    }

    private fun snapshot(): NotepadUiState = NotepadUiState(
        lines = document.lines.mapIndexed { index, line ->
            NotepadLineUiState(
                id = line.id,
                ordinal = index + 1,
                text = if (line.id == focused) draft else displayTextOf(line.id),
                evaluation = evaluation[line.id],
            )
        },
        focused = focused,
        caret = caret,
        textInputActive = textInputActive,
    )

    private fun displayTextOf(id: LineId): String = document.displayTextOf(id).orEmpty()

    private companion object {

        /**
         * Whether a line is written in words rather than in numbers.
         *
         * The first character decides, because that is the only one that exists when the
         * decision has to be made. `docs/ARCHITECTURE.md` §5.
         */
        fun prefersTextInput(text: String): Boolean = text.firstOrNull { !it.isWhitespace() }?.isLetter() == true
    }
}
