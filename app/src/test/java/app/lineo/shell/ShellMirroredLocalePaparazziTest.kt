package app.lineo.shell

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.LayoutDirection
import org.junit.Rule
import org.junit.Test

/**
 * The shell screens in `ar-XB`: expanded, and mirrored end to end (P1-08, P1-09).
 *
 * The other RTL snapshots flip the *layout* by overriding `LocalLayoutDirection`. This one
 * flips the resources as well — the strings arrive already reversed and bracketed, which is
 * what a real right-to-left locale hands a screen and what a layout-direction override alone
 * cannot show. It is the `ar-XB` run P1-08 asked for, taken where a screenshot can hold it
 * still rather than on a device whose system locale needs root to change.
 */
class ShellMirroredLocalePaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "b+ar+XB", layoutDirection = LayoutDirection.RTL),
    )

    @Test
    fun `settings in the right-to-left pseudo-locale`() {
        paparazzi.snapshot { Themed { RightToLeft { DefaultSettings() } } }
    }

    @Test
    fun `history in the right-to-left pseudo-locale`() {
        paparazzi.snapshot { Themed { RightToLeft { HistoryScreen(entries = TAPE, onReuse = {}, onClear = {}) } } }
    }
}
