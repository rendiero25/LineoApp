package app.lineo.notepad

import app.lineo.engine.CalcError
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
    evaluator: NotepadEvaluator = NotepadEvaluator(),
    decimalSeparator: Char = '.',
) {

    private var evaluator: NotepadEvaluator = evaluator

    private var decimalSeparator: Char = decimalSeparator

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
     * Reads the document again against a new context — a changed angle mode, or a changed
     * reading locale.
     *
     * Every line is recomputed, and deliberately: the old answers were read under rules that
     * no longer apply, and `sin(30)` means something different in RAD. The parse cache goes
     * with the old evaluator, since a cached parse is only valid for the scope it was read in.
     *
     * [decimalSeparator] travels with it, since both come from the same settings change and
     * `±` has to look for the character the keypad now types.
     *
     * A no-op when nothing changed, so a recomposition cannot cost a full re-evaluation.
     */
    fun evaluateWith(evaluator: NotepadEvaluator, decimalSeparator: Char = this.decimalSeparator) {
        this.decimalSeparator = decimalSeparator
        if (evaluator === this.evaluator) return
        this.evaluator = evaluator
        evaluation = evaluator.evaluate(document)
        state.value = snapshot()
    }

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
     * Moves the caret [lines] lines up or down, and says whether it went anywhere.
     *
     * What the arrow keys of a hardware keyboard do (P1-08b). Left and right belong to the
     * text field — they walk characters, and the field already knows how — but up and down
     * leave the line, and only the document knows what is above and below it.
     *
     * The column is kept where the new line is long enough for it, and clamped to the end
     * where it is not, which is what every editor does. `false` at the first line and the
     * last means the key was not ours: the caller lets the platform have it rather than
     * swallowing it silently.
     */
    fun moveFocusBy(lines: Int): Boolean {
        val id = focused ?: return false
        val index = document.lines.indexOfFirst { it.id == id }
        val target = document.lines.getOrNull(index + lines) ?: return false
        val column = caret
        focus(target.id, caret = column)
        return true
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
        val segments = text.split('\n')
        if (segments.size == 1) {
            write(id, text, caret)
            return
        }
        // A newline inside one line is not a line: it arrives from a paste, and the document
        // has a shape for it already. Storing it as typed would give one line two expressions
        // and one result, and the second half would never be evaluated.
        write(id, segments.first(), segments.first().length)
        segments.drop(1).forEach { segment -> appendBelowFocus(segment) }
        evaluate()
    }

    /** Puts [displayText] on a new line under the focused one, and moves the caret to it. */
    private fun appendBelowFocus(displayText: String) {
        val index = document.lines.indexOfFirst { it.id == focused }
        if (index < 0) return
        document = document.insertAt(index + 1, canonical(displayText))
        focused = document.lines[index + 1].id
        draft = displayText
        caret = displayText.length
    }

    /**
     * Replaces the name an error points at with the engine's nearest match — the fix chip.
     *
     * The line is focused first if it was not: the replacement is made against the draft, and
     * the draft only exists for the focused line. Tapping a chip on another line is therefore
     * a move of the caret as well as an edit, which is also what the user expects to see.
     */
    fun applySuggestion(id: LineId, error: CalcError.UnknownIdentifier) {
        val suggestion = error.suggestion ?: return
        if (focused != id) focus(id)
        if (focused != id) return
        val start = error.span.first.coerceIn(0, draft.length)
        val end = (error.span.last + 1).coerceIn(start, draft.length)
        write(id, draft.replaceRange(start, end, suggestion), start + suggestion.length)
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
                ast = evaluation.astOf(line.id),
            )
        },
        focused = focused,
        caret = caret,
        textInputActive = textInputActive,
        suggestions = suggestionsInScope(),
    )

    /**
     * The names in scope at the caret: the variables defined above the focused line, nearest
     * definition first, then the units the lines above have produced values in.
     *
     * Above and not below, because that is where a name is visible from — §3.1 and the
     * P1-02-1 decision. Nearest first because a name defined two lines up is the one being
     * worked with, while the one at the top of the document was finished with long ago; where
     * a name is defined twice the nearest also wins in the parser, so the chip inserts what
     * the line will actually read.
     *
     * Units come from what the document has already computed rather than from the unit
     * registry. Every unit Lineo knows would be hundreds of chips and none of them evidence
     * of what this user is working in.
     */
    private fun suggestionsInScope(): List<NotepadSuggestion> {
        val id = focused ?: return emptyList()
        val above = document.lines.takeWhile { it.id != id }.asReversed()
        val definitions = evaluation.graph.definitions
        val variables = above.mapNotNull { definitions[it.id] }
            .distinct()
            .map { NotepadSuggestion(it, NotepadSuggestion.Kind.Variable) }
        val units = above.mapNotNull { (evaluation[it.id] as? LineEvaluation.Value)?.value?.unit }
            .filterNot { it.isEmpty }
            .map { it.symbol }
            .distinct()
            .map { NotepadSuggestion(it, NotepadSuggestion.Kind.Unit) }
        return variables + units
    }

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
