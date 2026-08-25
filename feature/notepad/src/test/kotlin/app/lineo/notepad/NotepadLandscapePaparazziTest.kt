package app.lineo.notepad

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
 * The notepad on a phone held sideways — the window that had no snapshot, and the one the
 * bottom row of the keypad fell out of.
 *
 * Every other picture of this screen is either a portrait phone or a tablet, and both have
 * height to spare. A phone in landscape has the *width* of a tablet and the height of
 * nothing, which is the case `InputPane` exists to handle: the chip strip goes, its keys
 * reappear as the keypad's fifth column, and all five rows still fit at 48 dp. Before that,
 * `0` was drawn below the window and could not be pressed at all.
 *
 * The width class is provided, as everywhere else — it is the shell's to measure and a
 * snapshot has no window to ask. 851 dp of width reports Expanded on a device too.
 */
class NotepadLandscapePaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = PHONE_LANDSCAPE)

    @Test
    fun `the notepad on a phone in landscape`() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalWindowWidthClass provides WindowWidthClass.Expanded) {
                LineoTheme(darkTheme = false, dynamicColor = false) {
                    Box(modifier = Modifier.fillMaxSize().padding(top = ABOVE_THE_SCREEN)) {
                        NotepadScreen(state = notepad())
                    }
                }
            }
        }
    }

    private fun notepad(): NotepadState {
        val state = NotepadState(Documents.of(*LINES.toTypedArray()))
        state.focus(state.uiState.value.lines.last().id)
        return state
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

        val LINES = listOf("rate = 12500", "5 km + 300 m", "3 * rate")
    }
}
