package app.lineo.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.lineo.ui.layout.LocalWindowWidthClass
import app.lineo.ui.layout.WindowWidthClass
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
    // In a side pane the keypad has a ceiling; stacked under a document it does not.
    // Which dimension is scarce decides how a square key is sized, and getting it wrong
    // is visible: five square keys measured from the width overflowed a landscape pane.
    val heightIsScarce = LocalWindowWidthClass.current != WindowWidthClass.Compact
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (heightIsScarce) Modifier.fillMaxHeight() else Modifier)
            .background(RoleColors.of(LineoRole.Editor).container)
            .dockedBottomPadding()
            .padding(horizontal = LineoDimens.KeypadEdge, vertical = LineoDimens.KeyGap),
        verticalArrangement = Arrangement.spacedBy(LineoDimens.KeyGap),
    ) {
        ModeKey(key = state.modeKey, onPress = state::press)
        state.rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (heightIsScarce) Modifier.weight(1f) else Modifier),
                horizontalArrangement = Arrangement.spacedBy(
                    space = LineoDimens.KeyGap,
                    alignment = Alignment.CenterHorizontally,
                ),
            ) {
                row.forEach { key ->
                    Key(key = key, onPress = state::press, sizedByHeight = heightIsScarce)
                }
            }
        }
    }
}

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
 * One key.
 *
 * Every key takes an equal share of the row, so the grid stays aligned whatever the labels
 * are — a decimal separator that changes from `.` to `,` must not shift the column.
 */
@Composable
private fun RowScope.Key(key: KeypadKey, onPress: (KeypadKey) -> Unit, sizedByHeight: Boolean) {
    val colors = RoleColors.of(key.role)
    val description = key.contentDescription?.let { stringResource(it) }
    Box(
        modifier = Modifier
            // Keys are circles either way; what changes is which dimension sets the
            // diameter. Stacked under a document, width is the scarce one and the row
            // divides it. In a side pane the pane's height is the ceiling, so the key
            // takes the row's height and the row centres what is left over.
            .then(
                if (sizedByHeight) {
                    Modifier.fillMaxHeight()
                } else {
                    Modifier.weight(1f)
                },
            )
            .aspectRatio(1f)
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
