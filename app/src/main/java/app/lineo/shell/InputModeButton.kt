package app.lineo.shell

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.lineo.R
import app.lineo.ui.input.LocalInputModeToggle

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
 * The glyph is what changes with the state: the notepad while the keypad is up, the keypad
 * while the keyboard is. The description a screen reader gets names the same destination the
 * icon does, so neither depends on telling two colours apart (`docs/CONVENTIONS.md` §10).
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
    TopBarButton(description = description, onClick = toggle::toggle, modifier = modifier) {
        Icon(
            // The icon names the destination, as the `ABC` / `123` labels it replaces did:
            // showing the keypad while the keypad is up would say where you already are.
            painter = painterResource(if (active) R.drawable.ic_keypad else R.drawable.ic_notepad),
            contentDescription = null,
            tint = topBarButtonContent(),
            modifier = Modifier.size(TopBarIconSize),
        )
    }
}
