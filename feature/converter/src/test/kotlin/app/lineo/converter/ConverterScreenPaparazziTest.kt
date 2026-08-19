package app.lineo.converter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.ui.theme.LineoTheme
import org.junit.Rule
import org.junit.Test

/**
 * The converter in the schemes and the direction `AGENTS.md` §6 asks for.
 *
 * The snapshots are of the empty screen and cannot be otherwise: the amount lives in state the
 * screen creates, and driving it would mean hoisting that state outward for the benefit of a
 * picture. What the amount and the result look like once typed is `ConverterStateTest`'s, and
 * what a picture is actually protecting here is the layout — categories above, pickers below,
 * the keypad docked, and nothing pushed off the window.
 */
class ConverterScreenPaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `converter in light scheme`() {
        paparazzi.snapshot { Converter() }
    }

    @Test
    fun `converter in dark scheme`() {
        paparazzi.snapshot { Converter(dark = true) }
    }

    @Test
    fun `converter right to left`() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Converter() }
        }
    }

    @Composable
    private fun Converter(dark: Boolean = false) {
        LineoTheme(darkTheme = dark, dynamicColor = false) {
            ConverterScreen()
        }
    }
}
