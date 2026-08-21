package app.lineo.shell

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.data.settings.SeparatorPreference
import app.lineo.data.settings.UnitSystem
import app.lineo.data.settings.UserSettings
import app.lineo.engine.AngleMode
import org.junit.Rule
import org.junit.Test

/**
 * The three screens `:app` owns, in the schemes and the direction §10 and P1-08 name.
 *
 * These had no snapshot until the Paparazzi plugin was applied here — the gap recorded
 * against P1-06 and left open through P1-07. Everything they render is a value, so each one
 * is a pure function of the state passed in and the pictures are stable.
 *
 * The pseudo-locales live in classes of their own, because Paparazzi renders one device per
 * class: `ShellExpandedLocalePaparazziTest` and `ShellMirroredLocalePaparazziTest`.
 */
class ShellScreensPaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `settings`() {
        paparazzi.snapshot { Themed { DefaultSettings() } }
    }

    @Test
    fun `settings in dark scheme`() {
        paparazzi.snapshot { Themed(dark = true) { DefaultSettings() } }
    }

    @Test
    fun `settings right to left`() {
        paparazzi.snapshot { Themed { RightToLeft { DefaultSettings() } } }
    }

    @Test
    fun `settings with a comma separator chosen`() {
        // The selected chip is bolder as well as accented (§8): a colour-blind reader, and
        // this picture in greyscale, can both still tell which one is chosen.
        paparazzi.snapshot {
            Themed {
                SettingsScreen(
                    settings = UserSettings(
                        separator = SeparatorPreference.COMMA,
                        angleMode = AngleMode.RAD,
                        unitSystem = UnitSystem.IMPERIAL,
                        decimalPlaces = 2,
                    ),
                    actions = NO_ACTIONS,
                )
            }
        }
    }

    @Test
    fun `history`() {
        paparazzi.snapshot { Themed { HistoryScreen(entries = TAPE, onReuse = {}, onClear = {}) } }
    }

    @Test
    fun `history in dark scheme`() {
        paparazzi.snapshot { Themed(dark = true) { HistoryScreen(entries = TAPE, onReuse = {}, onClear = {}) } }
    }

    @Test
    fun `history right to left`() {
        paparazzi.snapshot {
            Themed { RightToLeft { HistoryScreen(entries = TAPE, onReuse = {}, onClear = {}) } }
        }
    }

    @Test
    fun `an empty tape says so`() {
        paparazzi.snapshot { Themed { HistoryScreen(entries = emptyList(), onReuse = {}, onClear = {}) } }
    }

    @Test
    fun `licences`() {
        paparazzi.snapshot { Themed { LicencesScreen(dependencies = LIBRARIES) } }
    }

    @Test
    fun `licences in dark scheme`() {
        paparazzi.snapshot { Themed(dark = true) { LicencesScreen(dependencies = LIBRARIES) } }
    }

    @Test
    fun `licences right to left`() {
        paparazzi.snapshot { Themed { RightToLeft { LicencesScreen(dependencies = LIBRARIES) } } }
    }
}
