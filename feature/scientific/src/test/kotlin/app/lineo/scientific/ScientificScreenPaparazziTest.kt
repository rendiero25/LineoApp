package app.lineo.scientific

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.ui.layout.LocalWindowWidthClass
import app.lineo.ui.layout.WindowWidthClass
import app.lineo.ui.theme.LineoTheme
import org.junit.Rule
import org.junit.Test

/**
 * The scientific screen in light, dark and RTL, as `AGENTS.md` §6 asks of a UI change.
 *
 * The wide snapshot is the one worth having: it is the only place the full grid appears, and
 * it is taken by providing the width class rather than by rotating a device, which is the
 * whole point of P1-04 being a width decision (`docs/ANDROID_STANDARDS.md` §2).
 */
class ScientificScreenPaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `scientific keypad in light scheme`() {
        paparazzi.snapshot { Scientific() }
    }

    @Test
    fun `scientific keypad in dark scheme`() {
        paparazzi.snapshot { Scientific(dark = true) }
    }

    @Test
    fun `scientific keypad right to left`() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Scientific() }
        }
    }

    @Composable
    private fun Scientific(dark: Boolean = false, width: WindowWidthClass = WindowWidthClass.Compact) {
        CompositionLocalProvider(LocalWindowWidthClass provides width) {
            LineoTheme(darkTheme = dark, dynamicColor = false) {
                ScientificScreen()
            }
        }
    }
}
