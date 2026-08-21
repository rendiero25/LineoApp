package app.lineo.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.data.model.HistoryEntry
import app.lineo.data.settings.SeparatorPreference
import app.lineo.data.settings.UnitSystem
import app.lineo.data.settings.UserSettings
import app.lineo.engine.AngleMode
import app.lineo.licenses.LicensedDependency
import app.lineo.ui.theme.LineoTheme
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * The three screens `:app` owns, in the schemes and the direction §10 and P1-08 name.
 *
 * These had no snapshot until the Paparazzi plugin was applied here — the gap recorded
 * against P1-06 and left open through P1-07. Everything they render is a value, so each one
 * is a pure function of the state passed in and the pictures are stable.
 *
 * The licences screen is given a short, fixed list rather than the generated one: the real
 * list is 149 entries long and grows with every dependency, which would make this a snapshot
 * of the allowlist rather than of the screen. What the screen does with a licence — name it,
 * and open its text — is the same for three rows as for a hundred and forty-nine.
 */
class ShellScreensPaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `settings`() {
        paparazzi.snapshot { Themed { Settings() } }
    }

    @Test
    fun `settings in dark scheme`() {
        paparazzi.snapshot { Themed(dark = true) { Settings() } }
    }

    @Test
    fun `settings right to left`() {
        paparazzi.snapshot { Themed { RightToLeft { Settings() } } }
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

    @Composable
    private fun Settings() {
        SettingsScreen(settings = UserSettings(), actions = NO_ACTIONS)
    }

    @Composable
    private fun Themed(dark: Boolean = false, content: @Composable () -> Unit) {
        LineoTheme(darkTheme = dark, dynamicColor = false) { content() }
    }

    @Composable
    private fun RightToLeft(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { content() }
    }

    private companion object {

        /** A screen under a camera changes nothing, so every action is a no-op. */
        val NO_ACTIONS = SettingsActions(
            onSeparator = {},
            onAngleMode = {},
            onTheme = {},
            onUnitSystem = {},
            onDecimalPlaces = {},
            onDynamicColor = {},
            onOpenLicences = {},
        )

        /** Newest first, as the repository hands them over. */
        val TAPE = listOf(
            HistoryEntry(id = 3, expression = "19 * 312 * 12", resultText = "71,136", createdAt = Instant.EPOCH),
            HistoryEntry(id = 2, expression = "1.5 + 1", resultText = "2.5", createdAt = Instant.EPOCH),
            HistoryEntry(id = 1, expression = "5 km to mi", resultText = "3.106855961 mi", createdAt = Instant.EPOCH),
        )

        val LIBRARIES = listOf(
            LicensedDependency(module = "androidx.activity:activity", licence = "Apache-2.0"),
            LicensedDependency(module = "androidx.compose.ui:ui", licence = "Apache-2.0"),
            LicensedDependency(module = "org.jetbrains.kotlin:kotlin-stdlib", licence = "Apache-2.0"),
        )
    }
}
