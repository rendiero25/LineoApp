package app.lineo.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
 * @param content the screen. It composes `AdaptivePane` itself, because which panes exist
 *   is a screen's decision and not the shell's.
 */
@Composable
fun LineoAppShell(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalWindowWidthClass provides rememberWindowWidthClass()) {
        LineoTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                content()
            }
        }
    }
}
