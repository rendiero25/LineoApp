package app.lineo.ui.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lineo.registry.EditorCommand
import app.lineo.ui.R
import app.lineo.ui.theme.LineoRole

/**
 * The calculator keypad as an [app.lineo.registry.InputSurface].
 *
 * Holds the only mutable thing about a keypad — which character its decimal key types —
 * and turns a press into an `EditorCommand`. It knows nothing about the editor, the
 * document, or what is currently on screen; `docs/ARCHITECTURE.md` §5 is what lets the
 * Phase 2 custom keypad replace it without the editor noticing.
 *
 * @param decimalSeparator the character the decimal key inserts. Locale-derived, and
 *   overridable in settings — `docs/CONVENTIONS.md` §1 keeps that a display-boundary
 *   decision, so the keypad is told rather than deciding.
 */
@Stable
class KeypadState(decimalSeparator: Char = '.') : CommandInputSurface() {

    /**
     * Changing this rebuilds the layout on the next frame, which is what P1-07 means by
     * the keypad following the separator setting immediately.
     */
    var decimalSeparator: Char by mutableStateOf(decimalSeparator)

    /** The grid, top row first. Derived, so a separator change is the only thing that rebuilds it. */
    val rows: List<List<KeypadKey>> by derivedStateOf { keypadRows(this.decimalSeparator) }

    /** Publishes what [key] means. The only way a press reaches the editor. */
    fun press(key: KeypadKey) {
        emit(key.command)
    }
}

/**
 * Remembers a [KeypadState] and keeps its separator in step with [decimalSeparator].
 *
 * The state survives recomposition but not the caller changing separator — that is an
 * assignment, not a new keypad, so the command stream and any collector stay attached.
 */
@Composable
fun rememberKeypadState(decimalSeparator: Char = '.'): KeypadState {
    val state = remember { KeypadState(decimalSeparator) }
    state.decimalSeparator = decimalSeparator
    return state
}

/**
 * The compact layout: four columns, five rows.
 *
 * Digits sit in the familiar phone-dial block so the hand can find them without reading;
 * operators run down the trailing edge where a thumb reaches; backspace is on the top row,
 * away from `=`. The `Aa` key raises the system keyboard, the hybrid-input switch of
 * `docs/ARCHITECTURE.md` §5. `=` emits `NewLine`, which is what committing a line means in
 * a notepad calculator — there is no separate result to compute on demand.
 *
 * **There is no all-clear key.** `EditorCommand` has no variant for it, and inventing one
 * would change the contract in `:core:registry` and `docs/ARCHITECTURE.md` §5. Recorded as
 * an open decision in `TASKS.md` rather than smuggled in as an empty `InsertText`.
 */
internal fun keypadRows(decimalSeparator: Char): List<List<KeypadKey>> = listOf(
    listOf(
        KeypadKey("⌫", LineoRole.Clear, EditorCommand.Backspace, R.string.key_backspace_description),
        KeypadKey("(", LineoRole.Function, EditorCommand.InsertText("("), R.string.key_open_bracket_description),
        KeypadKey(")", LineoRole.Function, EditorCommand.InsertText(")"), R.string.key_close_bracket_description),
        KeypadKey("÷", LineoRole.Operator, EditorCommand.InsertText("/"), R.string.key_divide_description),
    ),
    listOf(
        KeypadKey("7", LineoRole.Digit, EditorCommand.InsertText("7")),
        KeypadKey("8", LineoRole.Digit, EditorCommand.InsertText("8")),
        KeypadKey("9", LineoRole.Digit, EditorCommand.InsertText("9")),
        KeypadKey("×", LineoRole.Operator, EditorCommand.InsertText("*"), R.string.key_multiply_description),
    ),
    listOf(
        KeypadKey("4", LineoRole.Digit, EditorCommand.InsertText("4")),
        KeypadKey("5", LineoRole.Digit, EditorCommand.InsertText("5")),
        KeypadKey("6", LineoRole.Digit, EditorCommand.InsertText("6")),
        KeypadKey("−", LineoRole.Operator, EditorCommand.InsertText("-"), R.string.key_subtract_description),
    ),
    listOf(
        KeypadKey("1", LineoRole.Digit, EditorCommand.InsertText("1")),
        KeypadKey("2", LineoRole.Digit, EditorCommand.InsertText("2")),
        KeypadKey("3", LineoRole.Digit, EditorCommand.InsertText("3")),
        KeypadKey("+", LineoRole.Operator, EditorCommand.InsertText("+"), R.string.key_add_description),
    ),
    listOf(
        KeypadKey("Aa", LineoRole.Function, EditorCommand.ToggleTextInput, R.string.key_text_keyboard_description),
        KeypadKey("0", LineoRole.Digit, EditorCommand.InsertText("0")),
        KeypadKey(
            label = decimalSeparator.toString(),
            role = LineoRole.Digit,
            command = EditorCommand.InsertText(decimalSeparator.toString()),
            contentDescription = R.string.key_decimal_separator_description,
        ),
        KeypadKey("=", LineoRole.Equals, EditorCommand.NewLine, R.string.key_equals_description),
    ),
)
