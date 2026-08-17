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
import app.lineo.ui.layout.LocalWindowWidthClass
import app.lineo.ui.layout.WindowWidthClass
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
class KeypadState(
    decimalSeparator: Char = '.',
    hasRoomForFunctions: Boolean = false,
) : CommandInputSurface() {

    /**
     * Changing this rebuilds the layout on the next frame, which is what P1-07 means by
     * the keypad following the separator setting immediately.
     */
    var decimalSeparator: Char by mutableStateOf(decimalSeparator)

    /**
     * Whether the window is wide enough for the fifth column.
     *
     * Set from the window width class, never from orientation — `docs/ANDROID_STANDARDS.md`
     * §2 forbids branching on orientation, and the thing that actually decides is how much
     * width there is. A phone in landscape has room; so does a tablet held upright, and so
     * does an unfolded foldable. All three get the same keypad for the same reason.
     */
    var hasRoomForFunctions: Boolean by mutableStateOf(hasRoomForFunctions)

    /** The grid, top row first. Derived, so only a real change rebuilds it. */
    val rows: List<List<KeypadKey>> by derivedStateOf {
        keypadRows(this.decimalSeparator, this.hasRoomForFunctions)
    }

    /**
     * The surface switch, which sits outside the grid.
     *
     * Floating above the grid rather than occupying a cell, because it is the one control
     * that does not type: it changes the instrument. A key in the grid looks like a key,
     * and this is a mode.
     *
     * Labelled `ABC` rather than with an icon or an `Aa`: it is the label every software
     * keyboard on the platform uses for exactly this journey, and it names the destination
     * rather than the mechanism. `123` on the accessory row is the same key coming back.
     */
    val modeKey: KeypadKey = KeypadKey(
        label = "ABC",
        role = LineoRole.InputSwitch,
        command = EditorCommand.ToggleTextInput,
        contentDescription = R.string.key_text_keyboard_description,
    )

    /** Publishes what [key] means. The only way a press reaches the editor. */
    fun press(key: KeypadKey) {
        emit(key.command)
    }
}

/**
 * Remembers a [KeypadState] and keeps it in step with the separator and the window.
 *
 * The state survives recomposition and survives both of those changing — they are
 * assignments, not a new keypad, so the command stream and any collector stay attached.
 */
@Composable
fun rememberKeypadState(decimalSeparator: Char = '.'): KeypadState {
    val hasRoom = LocalWindowWidthClass.current != WindowWidthClass.Compact
    val state = remember { KeypadState(decimalSeparator, hasRoom) }
    state.decimalSeparator = decimalSeparator
    state.hasRoomForFunctions = hasRoom
    return state
}

/**
 * Four columns when the window is narrow, five when it is not.
 *
 * Digits sit in the familiar phone-dial block so the hand can find them without reading.
 * Operators run down the trailing edge where a thumb reaches. `AC` and `=` share the
 * primary colour, so §10 fixes them to opposite corners — leading top and trailing bottom —
 * and `⌫` is kept away from `=`, because two keys that destroy work should not neighbour
 * the one pressed most often.
 *
 * `ABC` is not here: it floats above the grid, being a mode rather than a key. `=` emits
 * `NewLine`, which is what committing a line means in a notepad calculator. `AC` clears the
 * line and not the document. `±` flips the sign of the number the caret is in.
 *
 * **The fifth column is what a narrow phone cannot afford.** Brackets, power, root and the
 * argument separator go there, inserted before the operator column so the operators keep
 * the trailing edge. On a compact window they are reachable only from the accessory row —
 * a real gap, recorded in `TASKS.md`, and the reason this column exists at all.
 */
internal fun keypadRows(
    decimalSeparator: Char,
    hasRoomForFunctions: Boolean = false,
): List<List<KeypadKey>> {
    val extras = listOf(
        KeypadKey("(", LineoRole.Function, EditorCommand.InsertText("("), R.string.key_open_bracket_description),
        KeypadKey(")", LineoRole.Function, EditorCommand.InsertText(")"), R.string.key_close_bracket_description),
        KeypadKey("^", LineoRole.Function, EditorCommand.InsertText("^"), R.string.key_power_description),
        KeypadKey(
            label = "√",
            role = LineoRole.Function,
            command = EditorCommand.InsertFunction(name = "sqrt", arity = 1),
            contentDescription = R.string.key_square_root_description,
        ),
        KeypadKey(
            label = argumentSeparatorFor(decimalSeparator).toString(),
            role = LineoRole.Function,
            command = EditorCommand.InsertText(argumentSeparatorFor(decimalSeparator).toString()),
            contentDescription = R.string.key_argument_separator_description,
        ),
    )
    return baseRows(decimalSeparator).mapIndexed { index, row ->
        if (hasRoomForFunctions) row.dropLast(1) + extras[index] + row.last() else row
    }
}

private fun baseRows(decimalSeparator: Char): List<List<KeypadKey>> = listOf(
    listOf(
        KeypadKey("AC", LineoRole.Clear, EditorCommand.ClearLine, R.string.key_all_clear_description),
        KeypadKey("⌫", LineoRole.Function, EditorCommand.Backspace, R.string.key_backspace_description),
        KeypadKey("%", LineoRole.Operator, EditorCommand.InsertText("%"), R.string.key_percent_description),
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
        KeypadKey("±", LineoRole.Digit, EditorCommand.ToggleSign, R.string.key_toggle_sign_description),
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
