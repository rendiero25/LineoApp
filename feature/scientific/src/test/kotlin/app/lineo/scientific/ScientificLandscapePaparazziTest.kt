package app.lineo.scientific

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.ui.layout.LocalWindowWidthClass
import app.lineo.ui.layout.WindowWidthClass
import app.lineo.ui.theme.LineoTheme
import com.android.resources.ScreenOrientation
import org.junit.Rule
import org.junit.Test

/**
 * The scientific screen on a phone held sideways — the hardest window this layout has.
 *
 * The tablet snapshot shows the full grid because a tablet has the height for it. A phone in
 * landscape has the same width and less than half the height, so `InputPane` sheds in order:
 * the chip strip first, then the wide function rows, and what is left scrolls rather than
 * being drawn below the window. This picture is that state, and a regression that puts the
 * unreachable rows back will show up in it.
 */
class ScientificLandscapePaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = PHONE_LANDSCAPE)

    @Test
    fun `the scientific keypad on a phone in landscape`() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalWindowWidthClass provides WindowWidthClass.Expanded) {
                LineoTheme(darkTheme = false, dynamicColor = false) {
                    Box(modifier = Modifier.fillMaxSize().padding(top = ABOVE_THE_SCREEN)) {
                        ScientificScreen()
                    }
                }
            }
        }
    }

    private companion object {

        /**
         * What the shell takes before a screen sees the window: the status bar on this device
         * and the 60 dp top bar under it.
         *
         * Padded here rather than left out, because leaving it out is what made the first take
         * of this snapshot useless — with the whole 393 dp the chip strip fitted, and the case
         * worth a picture is the one where it does not. `LineoAppShell` lives in `:app` and a
         * feature may not reach for it, so the space it costs is stated as a number.
         */
        val ABOVE_THE_SCREEN: Dp = 112.dp

        /** Pixel 5 turned sideways: 851 × 393 dp, which is a hand's width and no height at all. */
        val PHONE_LANDSCAPE: DeviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = DeviceConfig.PIXEL_5.screenHeight,
            screenHeight = DeviceConfig.PIXEL_5.screenWidth,
            orientation = ScreenOrientation.LANDSCAPE,
        )
    }
}
