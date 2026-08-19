package app.lineo.scientific

import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.ui.layout.LocalWindowWidthClass
import app.lineo.ui.layout.WindowWidthClass
import app.lineo.ui.theme.LineoTheme
import org.junit.Rule
import org.junit.Test

/**
 * The full grid, on a window that really is wide enough for it.
 *
 * A class of its own rather than a second rule beside the phone snapshots: two `Paparazzi`
 * rules in one class render on one thread and layoutlib refuses — "Acquiring different
 * scenes from same thread without releases".
 *
 * The device is a tablet and the width class is provided to match it. Forcing the class over
 * a phone canvas would draw five columns in the space of four and prove the opposite of what
 * this asserts. Nothing here asks about orientation (`docs/ANDROID_STANDARDS.md` §2) — a
 * phone in landscape reaches this same layout by having the same width.
 */
class ScientificScreenWidePaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_C)

    @Test
    fun `the full scientific grid`() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalWindowWidthClass provides WindowWidthClass.Expanded) {
                LineoTheme(darkTheme = false, dynamicColor = false) {
                    ScientificScreen()
                }
            }
        }
    }
}
