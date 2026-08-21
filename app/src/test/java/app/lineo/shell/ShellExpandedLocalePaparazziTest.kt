package app.lineo.shell

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * The shell screens in `en-XA`: every string longer, accented and bracketed (P1-09).
 *
 * The pseudo-locale aapt2 generates for the debug build. It is the only way to see a
 * translation overflow before a translation exists — a row that fits "Theme" and clips
 * "[Ţĥéḿé one two]" is a row that will clip in German, and finding that here costs nothing.
 *
 * A class of its own because Paparazzi renders one device per class: a second rule beside
 * the first fights it for the render thread and every test in the class fails.
 */
class ShellExpandedLocalePaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "b+en+XA"))

    @Test
    fun `settings in the expanded pseudo-locale`() {
        paparazzi.snapshot { Themed { DefaultSettings() } }
    }

    @Test
    fun `history in the expanded pseudo-locale`() {
        paparazzi.snapshot { Themed { HistoryScreen(entries = TAPE, onReuse = {}, onClear = {}) } }
    }

    @Test
    fun `licences in the expanded pseudo-locale`() {
        paparazzi.snapshot { Themed { LicencesScreen(dependencies = LIBRARIES) } }
    }
}
