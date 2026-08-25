package app.lineo.ui.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import app.lineo.registry.EditorCommand
import app.lineo.ui.R
import app.lineo.ui.theme.LineoRole

/**
 * The strip that sits above the system keyboard, as an [app.lineo.registry.InputSurface].
 *
 * When the user is typing words — a variable name, a unit, a label — the keypad is gone and
 * the system keyboard is up. What the text keyboard does not offer is the handful of
 * characters an expression needs: brackets, a power sign, a root, the argument separator.
 * The accessory row is those, and the way back to the keypad.
 *
 * A separate surface from [KeypadState] rather than a mode of it. They are on screen at
 * different times, carry different keys, and P0-13 has both implementing `InputSurface`
 * independently — the editor collects whichever is attached and cannot tell them apart.
 */
@Stable
class AccessoryRowState(decimalSeparator: Char = '.') : CommandInputSurface() {

    /** Left to right, in the order they are shown. */
    val keys: List<KeypadKey> = accessoryKeys(decimalSeparator)

    /**
     * Emits [command] to whoever is collecting this surface.
     *
     * Takes the command rather than the [KeypadKey] it came from. The key was only ever
     * unwrapped here, and [ExpressionChipRow] draws chips a screen contributes as well as
     * keys — those have a command and no key, so the command is the thing both have.
     */
    fun press(command: EditorCommand) {
        emit(command)
    }
}

@Composable
fun rememberAccessoryRowState(decimalSeparator: Char = LocalDecimalSeparator.current): AccessoryRowState =
    remember(decimalSeparator) { AccessoryRowState(decimalSeparator) }

/**
 * What the text keyboard cannot type.
 *
 * `√` inserts a call rather than a glyph: `sqrt(` with the caret inside, because the
 * character on its own is not something the parser accepts.
 *
 * The way back to the keypad is not here. It was a `123` chip on this row and an `ABC` key
 * on the keypad — one journey with two buttons, in two places that moved as the surface
 * swapped. Both are now the single switch in the top bar (`InputModeToggle`), which does not
 * move when the surface below it does.
 */
fun accessoryKeys(decimalSeparator: Char = '.'): List<KeypadKey> = listOf(
    KeypadKey("(", LineoRole.SuggestionChip, EditorCommand.InsertText("("), R.string.key_open_bracket_description),
    KeypadKey(")", LineoRole.SuggestionChip, EditorCommand.InsertText(")"), R.string.key_close_bracket_description),
    KeypadKey("^", LineoRole.SuggestionChip, EditorCommand.InsertText("^"), R.string.key_power_description),
    KeypadKey(
        label = "√",
        role = LineoRole.SuggestionChip,
        command = EditorCommand.InsertFunction(name = "sqrt", arity = 1),
        contentDescription = R.string.key_square_root_description,
    ),
    KeypadKey("%", LineoRole.SuggestionChip, EditorCommand.InsertText("%"), R.string.key_percent_description),
    KeypadKey(
        label = argumentSeparatorFor(decimalSeparator).toString(),
        role = LineoRole.SuggestionChip,
        command = EditorCommand.InsertText(argumentSeparatorFor(decimalSeparator).toString()),
        contentDescription = R.string.key_argument_separator_description,
    ),
)

/**
 * Which of [keys] a screen's chip row shows, for the window and the surface it is in.
 *
 * Three cases, and the middle one is the product decision of P1-15-3:
 *
 * - **A wide window** shows all of them. The pane has the room for a grid, and the argument
 *   separator belongs in it because no other surface types that character.
 * - **A portrait window with the keypad up** shows none. The row of notation above the digits
 *   is more than a phone should spend on keys that are either already a keypad key (`%`) or
 *   part of the fifth column a wider window adds (`(`, `)`, `^`, `√`). What the row draws
 *   there is whatever the screen suggests, and nothing when there is nothing to suggest.
 * - **The text keyboard up** shows all of them again, in any window: the keypad is gone, and
 *   this row is then the only surface that can type a bracket at all.
 *
 * @param wideWindow whether the window is wider than compact. From the width class and never
 *   from the orientation (`docs/ANDROID_STANDARDS.md` §2) — the caller has already asked.
 */
fun expressionKeysFor(
    keys: List<KeypadKey>,
    wideWindow: Boolean,
    textInputActive: Boolean,
): List<KeypadKey> = if (wideWindow || textInputActive) keys else emptyList()
