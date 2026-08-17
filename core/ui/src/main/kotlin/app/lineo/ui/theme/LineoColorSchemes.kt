package app.lineo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The fallback palette, used below API 31 and whenever the user turns dynamic colour off.
 *
 * **Generated, not hand-picked.** Every value below is the output of Material's HCT tonal
 * palette algorithm (the TonalSpot scheme, which is what the Material Theme Builder
 * produces by default) run on the Lineo seed:
 *
 * ```
 * seed  #4C5FD5   hue 279.3716   chroma 61.7567   tone 44.9766
 *
 * primary        TonalPalette(hue, chroma = 36)
 * secondary      TonalPalette(hue, chroma = 16)
 * tertiary       TonalPalette(hue + 60, chroma = 24)
 * neutral        TonalPalette(hue, chroma = 6)
 * neutralVariant TonalPalette(hue, chroma = 8)
 * error          TonalPalette(hue = 25, chroma = 84)
 * ```
 *
 * `docs/CONVENTIONS.md` §10 forbids hand-picking hex values, because a literal chosen by
 * eye breaks the tonal relationships the whole scheme depends on. To change the palette,
 * change the seed and regenerate — never edit a single value here.
 */
object LineoColorSchemes {

    /** Seed the tonal palettes are derived from. Recorded so the palette can be regenerated. */
    val Seed: Color = Color(0xFF4C5FD5)

    val Light: ColorScheme = lightColorScheme(
        primary = Color(0xFF525A92),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDDE0FF),
        onPrimaryContainer = Color(0xFF0C154B),
        inversePrimary = Color(0xFFBBC3FF),
        secondary = Color(0xFF5B5D71),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE0E1FA),
        onSecondaryContainer = Color(0xFF181A2C),
        tertiary = Color(0xFF77546D),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD7F2),
        onTertiaryContainer = Color(0xFF2D1127),
        error = Color(0xFFBA1B1B),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD4),
        onErrorContainer = Color(0xFF410001),
        background = Color(0xFFFBF8FF),
        onBackground = Color(0xFF1B1B20),
        surface = Color(0xFFFBF8FF),
        onSurface = Color(0xFF1B1B20),
        surfaceVariant = Color(0xFFE3E1EC),
        onSurfaceVariant = Color(0xFF46464E),
        surfaceTint = Color(0xFF525A92),
        inverseSurface = Color(0xFF303035),
        inverseOnSurface = Color(0xFFF2EFF7),
        outline = Color(0xFF767680),
        outlineVariant = Color(0xFFC6C5CF),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFFBF8FF),
        surfaceDim = Color(0xFFDBD9E0),
        surfaceContainer = Color(0xFFEFEDF5),
        surfaceContainerHigh = Color(0xFFE9E7EF),
        surfaceContainerHighest = Color(0xFFE3E1E9),
        surfaceContainerLow = Color(0xFFF5F2FA),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        primaryFixed = Color(0xFFDDE0FF),
        primaryFixedDim = Color(0xFFBBC3FF),
        onPrimaryFixed = Color(0xFF0C154B),
        onPrimaryFixedVariant = Color(0xFF3A4279),
        secondaryFixed = Color(0xFFE0E1FA),
        secondaryFixedDim = Color(0xFFC4C5DD),
        onSecondaryFixed = Color(0xFF181A2C),
        onSecondaryFixedVariant = Color(0xFF434559),
        tertiaryFixed = Color(0xFFFFD7F2),
        tertiaryFixedDim = Color(0xFFE6B9D7),
        onTertiaryFixed = Color(0xFF2D1127),
        onTertiaryFixedVariant = Color(0xFF5D3C54),
    )

    val Dark: ColorScheme = darkColorScheme(
        primary = Color(0xFFBBC3FF),
        onPrimary = Color(0xFF232C61),
        primaryContainer = Color(0xFF3A4279),
        onPrimaryContainer = Color(0xFFDDE0FF),
        inversePrimary = Color(0xFF525A92),
        secondary = Color(0xFFC4C5DD),
        onSecondary = Color(0xFF2D2F42),
        secondaryContainer = Color(0xFF434559),
        onSecondaryContainer = Color(0xFFE0E1FA),
        tertiary = Color(0xFFE6B9D7),
        onTertiary = Color(0xFF45263D),
        tertiaryContainer = Color(0xFF5D3C54),
        onTertiaryContainer = Color(0xFFFFD7F2),
        error = Color(0xFFFFB4A9),
        onError = Color(0xFF680003),
        errorContainer = Color(0xFF930006),
        onErrorContainer = Color(0xFFFFDAD4),
        background = Color(0xFF131318),
        onBackground = Color(0xFFE3E1E9),
        surface = Color(0xFF131318),
        onSurface = Color(0xFFE3E1E9),
        surfaceVariant = Color(0xFF46464E),
        onSurfaceVariant = Color(0xFFC6C5CF),
        surfaceTint = Color(0xFFBBC3FF),
        inverseSurface = Color(0xFFE3E1E9),
        inverseOnSurface = Color(0xFF303035),
        outline = Color(0xFF91909A),
        outlineVariant = Color(0xFF46464E),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF39393F),
        surfaceDim = Color(0xFF131318),
        surfaceContainer = Color(0xFF1F1F25),
        surfaceContainerHigh = Color(0xFF29292F),
        surfaceContainerHighest = Color(0xFF34343A),
        surfaceContainerLow = Color(0xFF1B1B20),
        surfaceContainerLowest = Color(0xFF0E0E13),
        primaryFixed = Color(0xFFDDE0FF),
        primaryFixedDim = Color(0xFFBBC3FF),
        onPrimaryFixed = Color(0xFF0C154B),
        onPrimaryFixedVariant = Color(0xFF3A4279),
        secondaryFixed = Color(0xFFE0E1FA),
        secondaryFixedDim = Color(0xFFC4C5DD),
        onSecondaryFixed = Color(0xFF181A2C),
        onSecondaryFixedVariant = Color(0xFF434559),
        tertiaryFixed = Color(0xFFFFD7F2),
        tertiaryFixedDim = Color(0xFFE6B9D7),
        onTertiaryFixed = Color(0xFF2D1127),
        onTertiaryFixedVariant = Color(0xFF5D3C54),
    )

    /**
     * True-black dark variant for OLED panels. **Stub — premium, ships in Phase 3**
     * (`docs/SPEC.md` §3). It exists now so that nothing in the design system assumes
     * exactly two schemes.
     *
     * Only the surface family is overridden. Pulling the accent tones down as well would
     * lose the contrast the containers rely on, so they stay as [Dark] has them; the
     * surface ladder is regenerated at tones 0/6/8/12/17 instead of 4/10/12/17/22.
     */
    val AmoledDark: ColorScheme = Dark.copy(
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        surfaceDim = Color(0xFF000000),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF131318),
        surfaceContainer = Color(0xFF17171D),
        surfaceContainerHigh = Color(0xFF1F1F25),
        surfaceContainerHighest = Color(0xFF29292F),
    )
}
