package app.lineo.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import app.lineo.ui.layout.LocalWindowWidthClass
import app.lineo.ui.theme.LineoTheme

/**
 * Everything between the window and a screen: theme, width class, and the top inset.
 *
 * The activity draws edge to edge, so the shell is what keeps content out from under the
 * system bars. It pads for the status bar here, once, and leaves the bottom to
 * `AdaptivePane`, which hands it to the input pane — the keypad has to sit against the
 * keyboard, and a shell that had already padded the bottom would push it a navigation bar
 * too high.
 *
 * The overflow button gets a row of its own rather than floating over the screen. Overlaid,
 * it collided with the expression: both want the top trailing corner, and the expression
 * grows towards it as the line gets longer. A reserved band costs the height of one button
 * and cannot collide with anything.
 *
 * @param content the screen. It composes `AdaptivePane` itself, because which panes exist
 *   is a screen's decision and not the shell's.
 */
@Composable
fun LineoAppShell(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalWindowWidthClass provides rememberWindowWidthClass()) {
        LineoTheme {
            // Painted before the inset padding, so the surface runs edge to edge and the
            // status bar sits on Lineo's colour rather than on the window's default white.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        OverflowMenuButton()
                    }
                    Box(modifier = Modifier.weight(1f)) { content() }
                }
            }
        }
    }
}
