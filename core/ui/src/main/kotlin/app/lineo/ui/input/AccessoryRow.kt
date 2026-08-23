package app.lineo.ui.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The strip of expression keys that docks above the system keyboard.
 *
 * An [AccessoryRowState] drawn as an [ExpressionChipRow], and nothing more. The drawing used
 * to live here in full, beside a byte-for-byte equivalent in `:feature:notepad` — same chip,
 * same 16 dp corner, same 48 dp floor — because a feature could not reach into another one
 * to share it. P1-15 moved the drawing into [ExpressionChipRow] and left this as the name
 * for one particular use of it: docked, always, because that is what "above the keyboard"
 * means.
 *
 * It uses the same [app.lineo.ui.layout.dockedBottomPadding] as the keypad, which is what
 * keeps it attached to the keyboard without jumping. The padding is the larger of the `ime`
 * and `navigationBars` insets, so the row sits on the keyboard when one is up and on the
 * navigation bar when it is not — and because both are snapshot state, it travels with the
 * keyboard's show and hide animation instead of teleporting when it ends.
 *
 * That is also why the row does not measure the keyboard itself. A keyboard that reports a
 * different height, animates for a different duration, or resizes as the user types — the
 * three ways GBoard, SwiftKey and Samsung Keyboard differ — changes the inset, and the row
 * follows the inset.
 */
@Composable
fun AccessoryRow(state: AccessoryRowState, modifier: Modifier = Modifier) {
    ExpressionChipRow(
        keys = state.keys,
        // Straight to the surface. `press` takes the key it was given so the state can decide
        // what a key means; here the command is already the decision.
        onCommand = state::press,
        modifier = modifier,
        docked = true,
    )
}
