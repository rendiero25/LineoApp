package app.lineo.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.lineo.ui.layout.dockedBottomPadding
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.LineoTypography
import app.lineo.ui.theme.RoleColors
import app.lineo.ui.theme.asExpression

/**
 * The calculator keypad, docked at the bottom of the window.
 *
 * Stateless in the Compose sense: everything it can change lives in [state], and a press
 * leaves through `state.commands` rather than through a callback the caller has to wire.
 *
 * The container is painted before the inset padding so its colour reaches the bottom of the
 * window while the keys stay above the navigation bar and above the keyboard — the contract
 * `AdaptivePane` states and the reason `dockedBottomPadding` exists.
 */
@Composable
fun Keypad(state: KeypadState, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(RoleColors.of(LineoRole.Editor).container)
            .dockedBottomPadding()
            .padding(horizontal = LineoDimens.KeypadEdge, vertical = LineoDimens.KeyGap),
    ) {
        val size = keySize(state.rows, this.maxWidth, this.maxHeight)
        val gap = gapFor(state.rows, this.maxWidth, size)
        Column(verticalArrangement = Arrangement.spacedBy(LineoDimens.KeyGap)) {
            ModeKey(key = state.modeKey, onPress = state::press)
            state.rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    // Spread, never centred. Where the height is the binding constraint the
                    // keys are narrower than their columns, and centring the leftover moved
                    // the whole grid inwards — `AC` no longer sat under `ABC`, which is the
                    // one alignment `KeypadState` promises. The slack goes into the gaps, so
                    // the leading key keeps the pane's edge and so does the trailing one.
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    row.forEach { key -> Key(key = key, onPress = state::press, size = size) }
                }
            }
        }
    }
}

/**
 * The gap between keys: whatever the row has left over once the keys have their size.
 *
 * At least [LineoDimens.KeyGap], which is what it works out to whenever the width is the
 * binding constraint — so the phone keypad is unchanged and only a grid sized by its height
 * spreads further apart. Spreading rather than centring is what keeps the leading key on the
 * pane's edge, under `ABC` and in line with the chip row above it.
 */
private fun gapFor(rows: List<List<KeypadKey>>, maxWidth: Dp, size: Dp): Dp {
    val columns = rows.maxOfOrNull { it.size } ?: return LineoDimens.KeyGap
    if (columns < 2) return LineoDimens.KeyGap
    return ((maxWidth - size * columns) / (columns - 1)).coerceAtLeast(LineoDimens.KeyGap)
}

/**
 * How wide a key is: the scarcer of what the width allows and what the height allows.
 *
 * Keys are circles, so one number decides both dimensions and the only question is which
 * constraint binds. Both do, in different windows, and each was found by getting it wrong:
 * five square keys measured from the width overflowed a landscape pane, and a scientific
 * grid measured the same way asked for more height than a phone has and clipped the
 * expression out of the window entirely.
 *
 * A pane with no ceiling — a keypad inside something scrollable, or a preview — falls back
 * to the width, which is the behaviour every existing snapshot was recorded with.
 */
internal fun keySize(rows: List<List<KeypadKey>>, maxWidth: Dp, maxHeight: Dp): Dp {
    val columns = rows.maxOfOrNull { it.size } ?: return LineoDimens.MinTouchTarget
    val gap = LineoDimens.KeyGap
    val byWidth = (maxWidth - gap * (columns - 1)) / columns
    if (maxHeight == Dp.Infinity) return byWidth.atLeastATarget()
    // The mode key sits above the grid and takes its own row's worth of height with it.
    val forRows = maxHeight - LineoDimens.MinTouchTarget - gap * rows.size
    val byHeight = forRows / rows.size
    return minOf(byWidth, byHeight).atLeastATarget()
}

/**
 * A floor of 48 dp, whatever the arithmetic above worked out (`docs/CONVENTIONS.md` §8).
 *
 * Shrinking to fit is the right instinct everywhere except through the minimum target: a key
 * too small to hit reliably is not a smaller key, it is a key that fails. A grid that cannot
 * fit at this size overflows its pane instead, which is visible and reportable — a 34 dp key
 * on a landscape phone was neither.
 */
private fun Dp.atLeastATarget(): Dp = coerceAtLeast(LineoDimens.MinTouchTarget)

/**
 * The surface switch, above the grid and the size of a chip rather than a key.
 *
 * Deliberately not a cell. Every key in the grid types something into the expression; this
 * one changes what you are typing with, and a control that sits apart is read as a mode
 * before its label is. It lines up with `AC` beneath it, the other key that acts on the
 * line as a whole rather than on a character in it.
 */
@Composable
private fun ModeKey(key: KeypadKey, onPress: (KeypadKey) -> Unit) {
    val colors = RoleColors.of(key.role)
    val description = key.contentDescription?.let { stringResource(it) }
    Box(
        modifier = Modifier
            .size(LineoDimens.MinTouchTarget)
            .clip(CircleShape)
            .background(colors.container)
            .clickable { onPress(key) }
            .semanticsLabel(description),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = key.label, style = MaterialTheme.typography.titleMedium, color = colors.content)
    }
}

/**
 * One key, at the size [keySize] decided for the whole grid.
 *
 * Every key is the same size whatever its label is, so the grid stays aligned — a decimal
 * separator that changes from `.` to `,` must not shift the column — and a row with fewer
 * keys than the widest one starts at the leading edge with the rest of them.
 */
@Composable
private fun Key(key: KeypadKey, onPress: (KeypadKey) -> Unit, size: Dp) {
    val colors = RoleColors.of(key.role)
    val description = key.contentDescription?.let { stringResource(it) }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.container)
            .clickable { onPress(key) }
            .semanticsLabel(description),
        contentAlignment = Alignment.Center,
    ) {
        // The label is a fraction of the key, not a fixed size. A fixed one only works at
        // one key size, and the keypad has two: a landscape pane shrinks the keys, and
        // 38 sp labels came out clipped — AC read as a triangle and ± as a plus.
        BoxWithConstraints(contentAlignment = Alignment.Center) {
            Text(
                text = key.label,
                style = LineoTypography.KeypadLabel
                    .asExpression()
                    .copy(fontSize = (maxWidth.value * LABEL_SHARE_OF_KEY).sp, lineHeight = TextUnit.Unspecified),
                color = colors.content,
                maxLines = 1,
            )
        }
    }
}

/**
 * How much of a key's diameter its label occupies.
 *
 * Chosen to reproduce the 38 sp of `LineoTypography.KeypadLabel` at the size a key takes on
 * a compact phone, so the portrait keypad is unchanged and every other window scales from
 * it. To make labels bigger, change this — not a size in one place and not the other.
 */
private const val LABEL_SHARE_OF_KEY = 0.42f
