package app.lineo.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RoleColors.of(LineoRole.Editor).container)
            .dockedBottomPadding()
            .padding(horizontal = LineoDimens.KeypadEdge, vertical = LineoDimens.KeyGap),
        verticalArrangement = Arrangement.spacedBy(LineoDimens.KeyGap),
    ) {
        ModeKey(key = state.modeKey, onPress = state::press)
        state.rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LineoDimens.KeyGap),
            ) {
                row.forEach { key -> Key(key = key, onPress = state::press) }
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
private fun RowScope.Key(key: KeypadKey, onPress: (KeypadKey) -> Unit) {
    val colors = RoleColors.of(key.role)
    val description = key.contentDescription?.let { stringResource(it) }
    Box(
        modifier = Modifier
            .weight(1f)
            // Square cell, circular key: the diameter follows the column width, so a
            // narrower phone gets smaller keys rather than an overflowing grid.
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(colors.container)
            .clickable { onPress(key) }
            .semanticsLabel(description),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = key.label,
            style = LineoTypography.KeypadLabel.asExpression(),
            color = colors.content,
        )
    }
}
