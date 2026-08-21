package app.lineo.ui.input

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.ui.theme.LineoTheme
import org.junit.Rule
import org.junit.Test

/**
 * The keypad at the largest font a user can ask for (P1-08b).
 *
 * Android's accessibility font sizes go to 2× on a Pixel, and `docs/ANDROID_STANDARDS.md` §2
 * requires the display and the keypad to survive the maximum without clipping. A key is a
 * circle sized from the pane, so its label is the part that can outgrow it — the reason this
 * is a picture and not an assertion about a number.
 */
class KeypadFontScalePaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(fontScale = MAXIMUM_FONT_SCALE))

    @Test
    fun `keypad at the maximum font scale`() {
        paparazzi.snapshot {
            LineoTheme(dynamicColor = false) { Keypad(state = KeypadState()) }
        }
    }

    @Test
    fun `the wide keypad at the maximum font scale`() {
        // Five columns, so each key is narrower and each label has less room to grow into.
        paparazzi.snapshot {
            LineoTheme(dynamicColor = false) {
                Keypad(state = KeypadState(hasRoomForFunctions = true))
            }
        }
    }

    private companion object {
        /** What the accessibility font-size slider reaches on a Pixel. */
        const val MAXIMUM_FONT_SCALE = 2f
    }
}
