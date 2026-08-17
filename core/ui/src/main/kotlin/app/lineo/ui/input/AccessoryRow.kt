package app.lineo.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.lineo.ui.layout.dockedBottomPadding
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.RoleColors
import app.lineo.ui.theme.asExpression

/**
 * The strip of expression keys that docks above the system keyboard.
 *
 * It uses the same [dockedBottomPadding] as the keypad, which is what keeps it attached to
 * the keyboard without jumping. The padding is the larger of the `ime` and `navigationBars`
 * insets, so the row sits on the keyboard when one is up and on the navigation bar when it
 * is not — and because both are snapshot state, it travels with the keyboard's show and
 * hide animation instead of teleporting when it ends.
 *
 * That is also why the row does not measure the keyboard itself. A keyboard that reports a
 * different height, animates for a different duration, or resizes as the user types — the
 * three ways GBoard, SwiftKey and Samsung Keyboard differ — changes the inset, and the row
 * follows the inset.
 */
@Composable
fun AccessoryRow(state: AccessoryRowState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RoleColors.of(LineoRole.Editor).container)
            .dockedBottomPadding()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = LineoDimens.KeyGap, vertical = LineoDimens.Grid),
        horizontalArrangement = Arrangement.spacedBy(LineoDimens.KeyGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.keys.forEach { key -> AccessoryKey(key = key, onPress = state::press) }
    }
}

/**
 * One accessory key.
 *
 * Sized to its label rather than to a share of the row, and the row scrolls. A fixed
 * division would shrink every key each time one is added, and this row is expected to grow
 * — P1-03 puts in-scope variables and recent units here.
 */
@Composable
private fun AccessoryKey(key: KeypadKey, onPress: (KeypadKey) -> Unit) {
    val colors = RoleColors.of(key.role)
    val description = key.contentDescription?.let { stringResource(it) }
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = LineoDimens.MinTouchTarget, minHeight = LineoDimens.MinTouchTarget)
            .clip(RoundedCornerShape(AccessoryCornerRadius))
            .background(colors.container)
            .clickable { onPress(key) }
            .padding(horizontal = LineoDimens.LineGap)
            .then(
                if (description == null) {
                    Modifier
                } else {
                    Modifier.semantics { contentDescription = description }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = key.label,
            style = MaterialTheme.typography.titleMedium.asExpression(),
            color = colors.content,
        )
    }
}

private val AccessoryCornerRadius = 16.dp
