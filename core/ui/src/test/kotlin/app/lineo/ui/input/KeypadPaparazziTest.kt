package app.lineo.ui.input

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.ui.theme.LineoTheme
import org.junit.Rule
import org.junit.Test

/**
 * The keypad and the accessory row, in the schemes and directions §10 and P1-08 care about.
 *
 * No insets here: Paparazzi has no system bars and no keyboard, so what these assert is the
 * grid, the token colours, and the labels. Inset behaviour is verified on a device, which is
 * what the P0-13 definition of done asks for and what a snapshot cannot stand in for.
 */
class KeypadPaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `keypad in light scheme`() {
        paparazzi.snapshot {
            LineoTheme(darkTheme = false, dynamicColor = false) {
                Keypad(state = KeypadState())
            }
        }
    }

    @Test
    fun `keypad in dark scheme`() {
        paparazzi.snapshot {
            LineoTheme(darkTheme = true, dynamicColor = false) {
                Keypad(state = KeypadState())
            }
        }
    }

    @Test
    fun `keypad with a comma decimal separator`() {
        // id-ID and de-DE type a comma. The column must not shift when the glyph changes.
        paparazzi.snapshot {
            LineoTheme(darkTheme = false, dynamicColor = false) {
                Keypad(state = KeypadState(decimalSeparator = ','))
            }
        }
    }

    @Test
    fun `keypad and accessory row right to left`() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LineoTheme(darkTheme = false, dynamicColor = false) {
                    Column {
                        AccessoryRow(state = AccessoryRowState())
                        Keypad(state = KeypadState())
                    }
                }
            }
        }
    }

    @Test
    fun `accessory row in light scheme`() {
        paparazzi.snapshot {
            LineoTheme(darkTheme = false, dynamicColor = false) {
                AccessoryRow(state = AccessoryRowState())
            }
        }
    }
}
