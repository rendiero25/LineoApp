package app.lineo.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.lineo.data.model.HistoryEntry
import app.lineo.data.settings.UserSettings
import app.lineo.licenses.LicensedDependency
import app.lineo.ui.theme.LineoTheme
import java.time.Instant

/**
 * What the shell screens are shown holding, for every snapshot that shows them.
 *
 * Shared because the pseudo-locale snapshots live in classes of their own: Paparazzi takes
 * one device per test class — two rules in one class fight over the render thread — so the
 * fixtures have to sit outside all three rather than be copied into each.
 */

/** A screen under a camera changes nothing, so every action is a no-op. */
internal val NO_ACTIONS = SettingsActions(
    onSeparator = {},
    onAngleMode = {},
    onTheme = {},
    onUnitSystem = {},
    onDecimalPlaces = {},
    onDynamicColor = {},
    onOpenLicences = {},
)

/** Newest first, as the repository hands them over. */
internal val TAPE = listOf(
    HistoryEntry(id = 3, expression = "19 * 312 * 12", resultText = "71,136", createdAt = Instant.EPOCH),
    HistoryEntry(id = 2, expression = "1.5 + 1", resultText = "2.5", createdAt = Instant.EPOCH),
    HistoryEntry(id = 1, expression = "5 km to mi", resultText = "3.106855961 mi", createdAt = Instant.EPOCH),
)

/**
 * Three libraries rather than the generated list.
 *
 * The real one is 149 entries and grows with every dependency, which would make the snapshot
 * a picture of the allowlist. What the screen does with a licence — name it, and open its
 * text — is the same for three rows as for a hundred and forty-nine.
 */
internal val LIBRARIES = listOf(
    LicensedDependency(module = "androidx.activity:activity", licence = "Apache-2.0"),
    LicensedDependency(module = "androidx.compose.ui:ui", licence = "Apache-2.0"),
    LicensedDependency(module = "org.jetbrains.kotlin:kotlin-stdlib", licence = "Apache-2.0"),
)

/** The settings screen on its defaults. */
@Composable
internal fun DefaultSettings() {
    SettingsScreen(settings = UserSettings(), actions = NO_ACTIONS)
}

/** Lineo's own palette, never the wallpaper's: a snapshot has no wallpaper to follow. */
@Composable
internal fun Themed(dark: Boolean = false, content: @Composable () -> Unit) {
    LineoTheme(darkTheme = dark, dynamicColor = false) { content() }
}

/**
 * The layout mirrored, as a right-to-left locale mirrors it on a device.
 *
 * Overridden rather than inferred: under layoutlib the composition takes its direction from
 * the configuration it is given, and a device config in `ar-XB` mirrors the *resources*
 * without mirroring the layout. On a device the two arrive together, so the snapshots put
 * them together too.
 */
@Composable
internal fun RightToLeft(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { content() }
}
