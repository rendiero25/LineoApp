package app.lineo.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.ui.input.InputModeBinding
import app.lineo.ui.input.InputModeToggle
import app.lineo.ui.input.LocalInputModeToggle
import org.junit.Rule
import org.junit.Test

/**
 * The surface switch in the top bar, which P1-15-3 moved there from above the keypad grid.
 *
 * Three pictures because §10 asks for three: the two states it can be in, and the mirrored
 * layout that proves it keeps the *leading* end rather than the left one. The overflow
 * button is in frame with it, since what the band has to show is that the two ends line up
 * with the keypad's edges and not with the window's.
 */
class InputModeButtonPaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `the switch points at the text keyboard while the keypad is up`() {
        paparazzi.snapshot { Themed { TopBar(textInputActive = false) } }
    }

    @Test
    fun `the switch points back at the keypad while the keyboard is up`() {
        // Filled as well as a different icon: a toggle has to say which state it is in, and
        // §10 forbids saying it in colour alone.
        paparazzi.snapshot { Themed(dark = true) { TopBar(textInputActive = true) } }
    }

    @Test
    fun `the switch keeps the leading end right to left`() {
        paparazzi.snapshot { Themed { RightToLeft { TopBar(textInputActive = false) } } }
    }

    /**
     * The band the shell draws, with a screen bound to the switch.
     *
     * `LineoAppShell` itself is not snapshotted here: it fills the window and would make
     * these three pictures a screen apiece, where what is being asserted is one row.
     */
    @Composable
    private fun TopBar(textInputActive: Boolean) {
        CompositionLocalProvider(LocalInputModeToggle provides remember { InputModeToggle() }) {
            InputModeBinding(textInputActive = textInputActive, onToggle = {})
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                InputModeButton()
                OverflowMenuButton()
            }
        }
    }
}
