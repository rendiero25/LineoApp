package app.lineo.shell

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * The shell screens at the largest font a user can ask for (P1-08b).
 *
 * Settings is the screen most at risk: it is the only one that is mostly words, and every
 * one of them sits in a row with a control beside it. A title that wraps is fine; a chip row
 * that pushes its own labels out of the window is not.
 */
class ShellFontScalePaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(fontScale = MAXIMUM_FONT_SCALE))

    @Test
    fun `settings at the maximum font scale`() {
        paparazzi.snapshot { Themed { DefaultSettings() } }
    }

    @Test
    fun `history at the maximum font scale`() {
        paparazzi.snapshot { Themed { HistoryScreen(entries = TAPE, onReuse = {}, onClear = {}) } }
    }

    private companion object {
        /** What the accessibility font-size slider reaches on a Pixel. */
        const val MAXIMUM_FONT_SCALE = 2f
    }
}
