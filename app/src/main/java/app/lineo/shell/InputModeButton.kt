package app.lineo.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.lineo.R
import app.lineo.ui.input.LocalInputModeToggle
import app.lineo.ui.theme.LineoDimens

/**
 * The switch between the keypad and the text keyboard, at the leading end of the top bar.
 *
 * It used to float above the keypad grid, which meant it was only there while the keypad
 * was: the way out and the way back were two different buttons in two different places, and
 * both of them moved when the surface swapped. Here it is one button that never moves, and
 * the surface below changes under it.
 *
 * Nothing is drawn on a screen with no input surface — the history and the settings — which
 * is what `InputModeToggle.bound` reports.
 *
 * A black disc with a white glyph in both schemes, and the glyph is what changes: the
 * notepad while the keypad is up, the keypad while the keyboard is. The description a screen
 * reader gets names the same destination the icon does.
 */
@Composable
internal fun InputModeButton(modifier: Modifier = Modifier) {
    val toggle = LocalInputModeToggle.current
    if (!toggle.bound) return

    val active = toggle.textInputActive
    val description = stringResource(
        if (active) {
            app.lineo.ui.R.string.key_numeric_keypad_description
        } else {
            app.lineo.ui.R.string.key_text_keyboard_description
        },
    )
    Box(
        modifier = modifier
            // Same band as the overflow button opposite it: `KeyGap` below, which is what the
            // keypad leaves above its first row of keys.
            .padding(horizontal = LineoDimens.EditorPadding, vertical = LineoDimens.KeyGap)
            .clip(CircleShape)
            .background(SwitchContainer)
            .clickable { toggle.toggle() }
            .size(LineoDimens.MinTouchTarget)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            // The icon names the destination, as the `ABC` / `123` labels it replaces did:
            // showing the keypad while the keypad is up would say where you already are. It
            // is also the *only* thing that changes with the state, which is what keeps the
            // button legible to anyone who cannot tell the two colours apart
            // (`docs/CONVENTIONS.md` §10 — never colour alone).
            painter = painterResource(if (active) R.drawable.ic_keypad else R.drawable.ic_notepad),
            contentDescription = null,
            tint = SwitchContent,
            modifier = Modifier.size(IconSize),
        )
    }
}

/**
 * Black disc, white glyph, in both schemes.
 *
 * Not a role from `RoleColors`: this button is the one control that has to read as *the* way
 * between the two surfaces, and a token that follows the scheme made it a pale chip on a pale
 * bar. The pair is fixed rather than themed so it is the same landmark in the dark scheme as
 * in the light one. Contrast is 21:1 either way, which is every threshold §10 names.
 */
private val SwitchContainer = Color.Black
private val SwitchContent = Color.White

private val IconSize = 24.dp
