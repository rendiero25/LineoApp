package app.lineo.ui.input

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lineo.registry.EditorCommand
import app.lineo.ui.layout.LocalWindowWidthClass
import app.lineo.ui.layout.WindowWidthClass
import app.lineo.ui.layout.dockedBottomInset
import app.lineo.ui.theme.LineoDimens

/**
 * Everything the user types with: the expression keys, and the keypad under them.
 *
 * One implementation, because there were two. The notepad and the scientific screen each had
 * the same twenty lines — which keys the strip shows, whether it is a grid or a row, who owns
 * the bottom inset — and they had already drifted apart once. What is a screen's own is the
 * chips it contributes and what a command means to it; both arrive as parameters.
 *
 * **The pane fits its keys or it scrolls; it never clips them.** A key is 48 dp and never
 * less (`docs/CONVENTIONS.md` §8), so when a short window cannot hold every row at that size
 * something has to give, and the order it gives in is stated here:
 *
 * 1. The strip's own keys go first. Where they will not fit, a wide window puts them back in
 *    the keypad's fifth column — `(`, `)`, `^`, `√` and the argument separator are exactly
 *    that column, so nothing becomes unreachable by dropping the strip.
 * 2. A module's extra rows go next: [KeypadState.hasRoomForFunctions] is *height*, not width,
 *    and a landscape phone has the width for the scientific grid and not the height.
 * 3. Only if the keypad alone still does not fit does the pane scroll, with the grid pinned
 *    to the height its 48 dp keys need. Found on a phone in landscape, where the bottom row —
 *    `0` among them — was drawn below the window and could not be pressed at all.
 *
 * @param textInputActive whether the system keyboard is the surface. The keypad is not drawn
 *   then, and the strip carries every expression key, since it is the only thing that can
 *   type them.
 * @param onCommand where a press goes — a chip's as well as a key's, so a screen has one
 *   handler and not two (`docs/ARCHITECTURE.md` §5).
 * @param trailing chips the screen contributes: names in scope, units it has produced. They
 *   come after the keys and are dropped with them when there is no room for a strip.
 */
@Composable
fun InputPane(
    keypad: KeypadState,
    accessory: AccessoryRowState,
    textInputActive: Boolean,
    onCommand: (EditorCommand) -> Unit,
    modifier: Modifier = Modifier,
    trailing: List<ExpressionChip> = emptyList(),
) {
    val wideWindow = LocalWindowWidthClass.current != WindowWidthClass.Compact
    // The keypad pads for the navigation bar and the keyboard from inside, so the height it
    // needs includes that inset. Left out of the arithmetic, the pane decided a grid fitted
    // and then drew it a navigation bar too tall.
    val inset = dockedBottomInset()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val available = this.maxHeight
        // Step 2 of the order above, before the floor is measured: a layout with extra rows
        // is asked for them only if they fit. `rows` is derived, so this reads back the grid
        // the flag has just chosen.
        keypad.hasRoomForFunctions = wideWindow
        if (!textInputActive && keypadFloor(keypad, inset) > available) {
            keypad.hasRoomForFunctions = false
        }

        val floor = if (textInputActive) 0.dp else keypadFloor(keypad, inset)
        val roomForStrip = available - floor >= STRIP_MIN_HEIGHT
        // Step 1: the strip's keys become the keypad's fifth column when the strip cannot be
        // drawn. On a compact window there is no fifth column to move them to — that is the
        // portrait decision of P1-15-3, and `expressionKeysFor` states it.
        keypad.hasExtraColumn = wideWindow && !roomForStrip

        val keys = if (roomForStrip) {
            expressionKeysFor(accessory.keys, wideWindow, textInputActive)
        } else {
            emptyList()
        }
        val chips = if (roomForStrip) trailing else emptyList()
        val overflows = floor + (if (keys.isEmpty() && chips.isEmpty()) 0.dp else STRIP_MIN_HEIGHT) > available

        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Step 3. Never the first answer: a keypad the user has to scroll is a worse
                // keypad, so this is what is left when both of the steps above have run.
                .then(if (overflows) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        ) {
            ExpressionStrip(
                keys = keys,
                chips = chips,
                wideWindow = wideWindow,
                textInputActive = textInputActive,
                onCommand = onCommand,
            )
            if (!textInputActive) {
                // Pinned only when the pane scrolls. Inside a scrolling column the height is
                // unbounded, and a keypad told it has all the room in the world measures its
                // keys from the width and draws them enormous.
                Keypad(
                    state = keypad,
                    modifier = if (overflows) Modifier.height(floor) else Modifier,
                )
            }
        }
    }
}

/**
 * The keys above the surface: a grid where there is width for one, a scrolling row where
 * there is not, and nothing at all when neither has anything to show.
 *
 * A grid because a wide pane can hold every key in one line at a readable size, and a row
 * because a compact one cannot and scrolling sideways is how it copes.
 */
@Composable
private fun ExpressionStrip(
    keys: List<KeypadKey>,
    chips: List<ExpressionChip>,
    wideWindow: Boolean,
    textInputActive: Boolean,
    onCommand: (EditorCommand) -> Unit,
) {
    if (keys.isEmpty() && chips.isEmpty()) return
    if (wideWindow) {
        ExpressionChipGrid(
            keys = keys,
            onCommand = onCommand,
            trailing = chips,
            columns = keys.size.coerceAtLeast(MIN_GRID_COLUMNS),
        )
    } else {
        ExpressionChipRow(
            keys = keys,
            onCommand = onCommand,
            trailing = chips,
            // The keypad below owns the inset when it is showing; when it is not, this row is
            // the bottom-most thing in the window and owns it instead.
            docked = textInputActive,
        )
    }
}

/**
 * The least height a keypad can be drawn in: every row at the 48 dp floor.
 *
 * Mirrors what `keySize` does with the height it is given — a row's worth of gap per row,
 * plus the pane's own padding above and below, plus the inset the keypad consumes from
 * inside. Given exactly this, `keySize` works out to [LineoDimens.MinTouchTarget].
 */
private fun keypadFloor(keypad: KeypadState, inset: Dp): Dp {
    val rows = keypad.rows.size
    return (LineoDimens.MinTouchTarget + LineoDimens.KeyGap) * rows + LineoDimens.KeyGap * 2 + inset
}

/** A strip is a chip and the padding around it, and never less. */
private val STRIP_MIN_HEIGHT: Dp = LineoDimens.MinTouchTarget + LineoDimens.Grid * 2

/**
 * How wide the grid is when the strip carries nothing but a screen's own chips.
 *
 * The keys decide the column count when there are keys, because the grid exists to put them
 * in one row. With none, three is the shape a handful of suggestions reads best in.
 */
private const val MIN_GRID_COLUMNS = 3
