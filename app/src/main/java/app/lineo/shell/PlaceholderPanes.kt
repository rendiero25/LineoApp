package app.lineo.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.lineo.ui.layout.dockedBottomPadding
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.RoleColors

/**
 * Empty panes in the right colours, standing in until the editor and keypad exist.
 *
 * Phase 0 has no editor (P0-14) and no keypad (P0-13), but the shell it hangs them from has
 * to be verified against real system bars, a real keyboard, and a real fold — which needs
 * something on screen with a measurable edge. These are that and nothing more.
 *
 * They carry no text on purpose: a placeholder string is a user-facing string that would
 * need a resource, a translation, and then a deletion. **Delete this file at P0-13.**
 */
@Composable
internal fun PlaceholderDocument() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RoleColors.of(LineoRole.Editor).container),
    )
}

/**
 * Stands in for the keypad, at roughly the height four rows of keys will occupy.
 *
 * The modifier order is the part worth copying into the real keypad: background first, then
 * the docked padding, then the height. Painting before padding lets the container run to the
 * bottom of the window while the keys stay above the navigation bar and above the keyboard.
 * Padding first would stop the colour at the top of the bar and leave a strip of editor
 * surface behind it.
 */
@Composable
internal fun PlaceholderInput() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(RoleColors.of(LineoRole.Digit).container)
            .dockedBottomPadding()
            .height(LineoDimens.KeyMinSize * KEYPAD_ROWS),
    )
}

private const val KEYPAD_ROWS = 4
