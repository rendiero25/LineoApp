package app.lineo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The palette, used below API 31 and whenever the user turns dynamic colour off.
 *
 * **Generated, not hand-picked.** These are the values of a Material Theme Builder export,
 * committed verbatim from `docs/LineoCP/ui/theme/Color.kt` — the tool `docs/CONVENTIONS.md`
 * §10 names. Olive and chartreuse: the accent is warm and the surfaces are warm with it,
 * which keeps a screen that is mostly numbers from reading as cold.
 *
 * To change the palette, export again and replace the whole file. Never edit one value by
 * eye — the tonal relationships are what the scheme is, and a single adjusted hex breaks
 * them silently.
 *
 * The export also carries medium- and high-contrast variants of both schemes, which are
 * what the Android 14 contrast setting needs. They are not wired up yet; see the open row
 * for P0-12 in `TASKS.md`.
 */
object LineoColorSchemes {

    val Light: ColorScheme = lightColorScheme(
        primary = Color(0xFF646116),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEBE68D),
        onPrimaryContainer = Color(0xFF4B4900),
        secondary = Color(0xFF626042),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE8E4BE),
        onSecondaryContainer = Color(0xFF4A482C),
        tertiary = Color(0xFF3E6655),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFC0ECD6),
        onTertiaryContainer = Color(0xFF264E3E),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
        background = Color(0xFFFEF9EB),
        onBackground = Color(0xFF1D1C14),
        surface = Color(0xFFFEF9EB),
        onSurface = Color(0xFF1D1C14),
        surfaceVariant = Color(0xFFE7E3D1),
        onSurfaceVariant = Color(0xFF49473A),
        outline = Color(0xFF7A7768),
        outlineVariant = Color(0xFFCAC7B5),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF323128),
        inverseOnSurface = Color(0xFFF5F1E3),
        inversePrimary = Color(0xFFCFCA74),
        surfaceDim = Color(0xFFDEDACD),
        surfaceBright = Color(0xFFFEF9EB),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF8F4E6),
        surfaceContainer = Color(0xFFF2EEE0),
        surfaceContainerHigh = Color(0xFFECE8DB),
        surfaceContainerHighest = Color(0xFFE6E2D5),
        surfaceTint = Color(0xFF646116),
        primaryFixed = Color(0xFFEBE68D),
        primaryFixedDim = Color(0xFFCFCA74),
        onPrimaryFixed = Color(0xFF4B4900),
        onPrimaryFixedVariant = Color(0xFF646116),
        secondaryFixed = Color(0xFFE8E4BE),
        secondaryFixedDim = Color(0xFFCCC8A4),
        onSecondaryFixed = Color(0xFF4A482C),
        onSecondaryFixedVariant = Color(0xFF626042),
        tertiaryFixed = Color(0xFFC0ECD6),
        tertiaryFixedDim = Color(0xFFA5D0BB),
        onTertiaryFixed = Color(0xFF264E3E),
        onTertiaryFixedVariant = Color(0xFF3E6655),
    )

    val Dark: ColorScheme = darkColorScheme(
        primary = Color(0xFFCFCA74),
        onPrimary = Color(0xFF343200),
        primaryContainer = Color(0xFF4B4900),
        onPrimaryContainer = Color(0xFFEBE68D),
        secondary = Color(0xFFCCC8A4),
        onSecondary = Color(0xFF333118),
        secondaryContainer = Color(0xFF4A482C),
        onSecondaryContainer = Color(0xFFE8E4BE),
        tertiary = Color(0xFFA5D0BB),
        onTertiary = Color(0xFF0D3728),
        tertiaryContainer = Color(0xFF264E3E),
        onTertiaryContainer = Color(0xFFC0ECD6),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF14140C),
        onBackground = Color(0xFFE6E2D5),
        surface = Color(0xFF14140C),
        onSurface = Color(0xFFE6E2D5),
        surfaceVariant = Color(0xFF49473A),
        onSurfaceVariant = Color(0xFFCAC7B5),
        outline = Color(0xFF949181),
        outlineVariant = Color(0xFF49473A),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE6E2D5),
        inverseOnSurface = Color(0xFF323128),
        inversePrimary = Color(0xFF646116),
        surfaceDim = Color(0xFF14140C),
        surfaceBright = Color(0xFF3B3930),
        surfaceContainerLowest = Color(0xFF0F0E07),
        surfaceContainerLow = Color(0xFF1D1C14),
        surfaceContainer = Color(0xFF212018),
        surfaceContainerHigh = Color(0xFF2B2A22),
        surfaceContainerHighest = Color(0xFF36352C),
        surfaceTint = Color(0xFFCFCA74),
        // Identical to the light scheme, which is what "fixed" means: an accent that does
        // not flip when the scheme does. AC and = are painted from these.
        primaryFixed = Color(0xFFEBE68D),
        primaryFixedDim = Color(0xFFCFCA74),
        onPrimaryFixed = Color(0xFF4B4900),
        onPrimaryFixedVariant = Color(0xFF646116),
        secondaryFixed = Color(0xFFE8E4BE),
        secondaryFixedDim = Color(0xFFCCC8A4),
        onSecondaryFixed = Color(0xFF4A482C),
        onSecondaryFixedVariant = Color(0xFF626042),
        tertiaryFixed = Color(0xFFC0ECD6),
        tertiaryFixedDim = Color(0xFFA5D0BB),
        onTertiaryFixed = Color(0xFF264E3E),
        onTertiaryFixedVariant = Color(0xFF3E6655),
    )

    /**
     * True-black dark variant for OLED panels. **Premium, ships in Phase 3**
     * (`docs/SPEC.md` §3). It exists now so nothing in the design system assumes exactly
     * two schemes.
     *
     * Only the surface family moves. The accents keep the contrast their containers were
     * built for; the surface ladder slides one step down so that black is the floor and the
     * steps above it stay evenly spaced rather than bunching against it.
     */
    val AmoledDark: ColorScheme = Dark.copy(
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        surfaceDim = Color(0xFF000000),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF14140C),
        surfaceContainer = Color(0xFF1D1C14),
        surfaceContainerHigh = Color(0xFF212018),
        surfaceContainerHighest = Color(0xFF2B2A22),
    )
}
