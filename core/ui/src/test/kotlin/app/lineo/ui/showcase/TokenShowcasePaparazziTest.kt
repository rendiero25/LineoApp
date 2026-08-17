package app.lineo.ui.showcase

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.ui.theme.LineoTheme
import org.junit.Rule
import org.junit.Test

/**
 * The design system's regression net: the token showcase in light, dark, and RTL.
 *
 * Dynamic colour is switched off in every case. Paparazzi renders without a wallpaper, so
 * the dynamic path would produce whatever layoutlib defaults to and the snapshot would
 * assert nothing about Lineo's own palette. The seeded scheme is also the one that needs
 * watching: it is committed data, and a diff here means somebody edited a generated value.
 */
class TokenShowcasePaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `token showcase in light scheme`() {
        paparazzi.snapshot {
            LineoTheme(darkTheme = false, dynamicColor = false) {
                TokenShowcase()
            }
        }
    }

    @Test
    fun `token showcase in dark scheme`() {
        paparazzi.snapshot {
            LineoTheme(darkTheme = true, dynamicColor = false) {
                TokenShowcase()
            }
        }
    }

    @Test
    fun `token showcase in true black scheme`() {
        paparazzi.snapshot {
            LineoTheme(darkTheme = true, dynamicColor = false, trueBlack = true) {
                TokenShowcase()
            }
        }
    }

    @Test
    fun `token showcase right to left`() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LineoTheme(darkTheme = false, dynamicColor = false) {
                    TokenShowcase()
                }
            }
        }
    }
}
