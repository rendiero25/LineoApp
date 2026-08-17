package app.lineo.ui.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.Engine
import app.lineo.engine.EvalContext
import app.lineo.engine.Quantity
import app.lineo.registry.EditorCommand
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map

/**
 * The line being edited: its text, its caret, and what the engine makes of it.
 *
 * A plain state holder, not a `ViewModel` — `docs/ANDROID_STANDARDS.md` §1 keeps those at
 * screen level, and the editor is a component a notepad will place once per line.
 *
 * Every change arrives as an `EditorCommand`, whatever typed it. The keypad, the accessory
 * row, the system keyboard adapter and a suggestion chip all funnel through [apply], so
 * there is exactly one place where text changes and exactly one place to look when it
 * changes wrongly.
 *
 * @param decimalSeparator what counts as part of a number when `±` looks for one. Comes
 *   from the same setting the keypad reads (`docs/CONVENTIONS.md` §2).
 * @param evaluate seam for tests. Defaults to the real engine; a test can hand in something
 *   that counts its calls, which is how the debounce is asserted without waiting 400 ms.
 *   Last, so it can stay a trailing lambda.
 */
@Stable
class EditorState(
    private val decimalSeparator: Char = '.',
    private val evaluate: (String) -> CalcResult<Quantity> = { Engine.evaluate(it, EvalContext()) },
) {

    /**
     * What the user has typed, mirrored into a flow the evaluation pipeline reads.
     *
     * Two representations of one value, written in one place. The Compose state is what the
     * editor renders from; the flow is what [evaluationLoop] debounces. `snapshotFlow` would
     * have avoided the duplication, but it only sees a change once the recomposer sends
     * apply notifications — so the pipeline would depend on something being composed, and
     * would quietly stop evaluating whenever nothing was. The mirror has no such condition.
     */
    private val textFlow = MutableStateFlow("")

    /** What the user has typed. */
    var text: String by mutableStateOf("")
        private set

    /** Where the next insertion lands, as an offset into [text]. Always within bounds. */
    var caret: Int by mutableStateOf(0)
        private set

    /** What the engine last said. Updated by [evaluationLoop], never synchronously by [apply]. */
    var evaluation: EditorEvaluation by mutableStateOf(EditorEvaluation.Empty)
        private set

    /** Replaces the text wholesale — the system keyboard typing directly into the field. */
    fun setText(newText: String, newCaret: Int = newText.length) {
        text = newText
        textFlow.value = newText
        caret = newCaret.coerceIn(0, newText.length)
    }

    /**
     * Applies one command.
     *
     * Deliberately does not evaluate: evaluation is debounced, and a command that ran the
     * engine synchronously would make every keystroke pay for a parse.
     */
    fun apply(command: EditorCommand) {
        when (command) {
            is EditorCommand.InsertText -> insert(command.text)
            is EditorCommand.InsertFunction -> insertFunction(command.name)
            is EditorCommand.WrapSelection -> insert(command.open + command.close, caretOffset = -command.close.length)
            is EditorCommand.MoveCursor -> caret = (caret + command.delta).coerceIn(0, text.length)
            EditorCommand.Backspace -> backspace()
            EditorCommand.ClearLine -> setText("")
            EditorCommand.ToggleSign -> {
                val toggled = toggleSign(text, caret, decimalSeparator)
                setText(toggled.text, toggled.caret)
            }
            EditorCommand.NewLine -> insert("\n")
            // The surface swap is the shell's business; the text does not change.
            EditorCommand.ToggleTextInput -> Unit
        }
    }

    /**
     * Collects [commands] into this state. Runs for as long as the editor is on screen.
     */
    suspend fun consume(commands: Flow<EditorCommand>) {
        commands.collect(::apply)
    }

    /**
     * Keeps [evaluation] in step with [text], 400 ms after the user stops typing.
     *
     * `docs/SPEC.md` wants the result to feel live without the engine running on every
     * keystroke. Debouncing the *text* rather than the keystrokes means a burst of presses
     * costs one parse, and a paste costs one too.
     */
    @OptIn(FlowPreview::class)
    suspend fun evaluationLoop(debounceMillis: Long = DEBOUNCE_MILLIS) {
        textFlow
            .debounce(debounceMillis)
            .map(::evaluateNow)
            .collect { evaluation = it }
    }

    /**
     * Replaces a misspelled name with the engine's nearest match — the tap-to-fix chip.
     *
     * Takes the error rather than two offsets so the caret cannot be applied to a line that
     * has moved on since the suggestion was offered: the span belongs to the error, and the
     * chip is only shown while that error is the current one.
     */
    fun applySuggestion(error: CalcError.UnknownIdentifier) {
        val suggestion = error.suggestion ?: return
        val start = error.span.first.coerceIn(0, text.length)
        val end = (error.span.last + 1).coerceIn(start, text.length)
        setText(
            newText = text.replaceRange(start, end, suggestion),
            newCaret = start + suggestion.length,
        )
    }

    /**
     * Evaluates now and publishes the answer, skipping the debounce.
     *
     * What `=` does: the user has said they are finished, so making them wait another
     * 400 ms to see the result they just asked for would be perverse.
     */
    fun evaluateAndPublish() {
        evaluation = evaluateNow()
    }

    /** Evaluates immediately without publishing. */
    fun evaluateNow(source: String = text): EditorEvaluation = when {
        source.isBlank() -> EditorEvaluation.Empty
        else -> when (val result = evaluate(source)) {
            is CalcResult.Ok -> EditorEvaluation.Result(result.value)
            is CalcResult.Err ->
                if (isUnfinished(result.error, source)) {
                    EditorEvaluation.Unfinished
                } else {
                    EditorEvaluation.Failure(result.error)
                }
        }
    }

    private fun insert(inserted: String, caretOffset: Int = 0) {
        val at = caret.coerceIn(0, text.length)
        setText(
            newText = text.substring(0, at) + inserted + text.substring(at),
            newCaret = at + inserted.length + caretOffset,
        )
    }

    /** `sqrt(` with the caret between the brackets, where the argument goes. */
    private fun insertFunction(name: String) {
        insert("$name()", caretOffset = -1)
    }

    private fun backspace() {
        val at = caret.coerceIn(0, text.length)
        if (at == 0) return
        setText(newText = text.removeRange(at - 1, at), newCaret = at - 1)
    }

    private companion object {
        /**
         * Long enough that typing a number does not trigger a parse per digit, short enough
         * that the result feels attached to the keystroke. `docs/SPEC.md` fixes the value.
         */
        const val DEBOUNCE_MILLIS = 400L
    }
}
