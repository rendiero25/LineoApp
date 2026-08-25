package app.lineo.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.lineo.ui.editor.LocalAngleMode
import app.lineo.ui.format.LocalNumberLocale
import app.lineo.ui.format.LocalQuantityFormat
import app.lineo.ui.format.QuantityFormat
import app.lineo.ui.input.InputModeToggle
import app.lineo.ui.input.LocalDecimalSeparator
import app.lineo.ui.input.LocalInputModeToggle
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
 * @param overflow what sits in that band. The shell reserves the space; what the button
 *   opens is the host's, since only the host knows which destinations exist.
 * @param leading the other end of the band. By default the surface switch, which is drawn
 *   only on a screen that bound itself to it — see `InputModeToggle`.
 * @param settings the settings resolved into values — reading locale, separator, decimal
 *   places, angle mode and whether the wallpaper decides the colours. Provided to everything
 *   below, so a screen reads what the user chose rather than what the device happens to say.
 * @param darkTheme resolved from the theme setting by the caller, since only it knows what
 *   `SYSTEM` means on this device.
 * @param content the screen. It composes `AdaptivePane` itself, because which panes exist
 *   is a screen's decision and not the shell's.
 */
@Composable
fun LineoAppShell(
    settings: ResolvedSettings = ResolvedSettings.default(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    leading: @Composable () -> Unit = { InputModeButton() },
    overflow: @Composable () -> Unit = { OverflowMenuButton() },
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalWindowWidthClass provides rememberWindowWidthClass(),
        // The surface switch, which the top bar draws and a screen binds to. It is created
        // here because the bar is above every screen, and an ancestor cannot read state a
        // descendant holds — see `InputModeToggle`.
        LocalInputModeToggle provides remember { InputModeToggle() },
        // Both ends of the boundary `docs/CONVENTIONS.md` §1 draws, resolved once and from the
        // same settings: the locale an expression is read in, the character the decimal key
        // types, and how a result is written. A screen that took them as parameters would
        // carry a setting it does not decide; a screen that read the device configuration
        // instead would ignore the setting altogether.
        LocalNumberLocale provides settings.locale,
        LocalAngleMode provides settings.angleMode,
        LocalDecimalSeparator provides settings.decimalSeparator,
        LocalQuantityFormat provides remember(settings) {
            QuantityFormat(settings.locale, settings.decimalSeparator, settings.decimalPlaces)
        },
    ) {
        LineoTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        leading()
                        overflow()
                    }
                    Box(modifier = Modifier.weight(1f)) { content() }
                }
            }
        }
    }
}
