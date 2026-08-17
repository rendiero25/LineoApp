package app.lineo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The palette, used below API 31 and whenever the user turns dynamic colour off.
 *
 * **Generated, not hand-picked.** Surfaces, neutrals, tertiary and error come verbatim from
 * the Material Theme Builder export in `docs/LineoCP/ui/theme/Color.kt`. Olive and
 * chartreuse: the accent is warm and the surfaces are warm with it, which keeps a screen
 * that is mostly numbers from reading as cold.
 *
 * **The accent families are the same hue at a higher chroma.** The export's TonalSpot
 * variant builds primary at chroma 36 and secondary at 16, which is muted by design — at
 * those values the yellow reads as pale olive and the operator keys barely separate from
 * the digits. Both are regenerated from the same hue, 108.671°, using the HCT algorithm the
 * builder itself uses: primary at chroma 64, secondary at 32.
 *
 * **One deliberate deviation from the Material tone slots.** The light `secondaryContainer`
 * is T80, not the T90 Material specifies. At T90 the operator keys land at the same
 * lightness as the digits and, once the chroma was raised, at almost the same colour as the
 * yellow of `AC` and `=`. T80 puts them a step down: three levels the eye separates without
 * effort — bright yellow for the two keys that end a calculation, khaki for the operators,
 * neutral grey for the digits. Every other slot is Material's.
 *
 * To change any of it, regenerate — never edit one value by eye. The tonal relationships
 * are what a scheme is, and a single adjusted hex breaks them silently.
 *
 * The export also carries medium- and high-contrast variants of both schemes, which are
 * what the Android 14 contrast setting needs. They are not wired up yet; see the open row
 * for P0-12 in `TASKS.md`.
 */
object LineoColorSchemes {

    val Light: ColorScheme = lightColorScheme(
        primary = Color(0xFF656000),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEFE835),
        onPrimaryContainer = Color(0xFF1E1C00),
        secondary = Color(0xFF626042),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCECA7E),
        onSecondaryContainer = Color(0xFF1E1C00),
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
        inversePrimary = Color(0xFFD2CB0C),
        surfaceDim = Color(0xFFDEDACD),
        surfaceBright = Color(0xFFFEF9EB),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF8F4E6),
        surfaceContainer = Color(0xFFF2EEE0),
        surfaceContainerHigh = Color(0xFFECE8DB),
        surfaceContainerHighest = Color(0xFFE6E2D5),
        surfaceTint = Color(0xFF656000),
        primaryFixed = Color(0xFFEFE835),
        primaryFixedDim = Color(0xFFD2CB0C),
        onPrimaryFixed = Color(0xFF1E1C00),
        onPrimaryFixedVariant = Color(0xFF4C4800),
        secondaryFixed = Color(0xFFEAE697),
        secondaryFixedDim = Color(0xFFCECA7E),
        onSecondaryFixed = Color(0xFF1E1C00),
        onSecondaryFixedVariant = Color(0xFF4B490A),
        tertiaryFixed = Color(0xFFC0ECD6),
        tertiaryFixedDim = Color(0xFFA5D0BB),
        onTertiaryFixed = Color(0xFF264E3E),
        onTertiaryFixedVariant = Color(0xFF3E6655),
    )

    val Dark: ColorScheme = darkColorScheme(
        primary = Color(0xFFD2CB0C),
        onPrimary = Color(0xFF353200),
        primaryContainer = Color(0xFF4C4800),
        onPrimaryContainer = Color(0xFFEFE835),
        secondary = Color(0xFFCCC8A4),
        onSecondary = Color(0xFF333118),
        secondaryContainer = Color(0xFF4B490A),
        onSecondaryContainer = Color(0xFFEAE697),
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
        inversePrimary = Color(0xFF656000),
        surfaceDim = Color(0xFF14140C),
        surfaceBright = Color(0xFF3B3930),
        surfaceContainerLowest = Color(0xFF0F0E07),
        surfaceContainerLow = Color(0xFF1D1C14),
        surfaceContainer = Color(0xFF212018),
        surfaceContainerHigh = Color(0xFF2B2A22),
        surfaceContainerHighest = Color(0xFF36352C),
        surfaceTint = Color(0xFFD2CB0C),
        // Identical to the light scheme, which is what "fixed" means: an accent that does
        // not flip when the scheme does. AC and = are painted from these.
        primaryFixed = Color(0xFFEFE835),
        primaryFixedDim = Color(0xFFD2CB0C),
        onPrimaryFixed = Color(0xFF1E1C00),
        onPrimaryFixedVariant = Color(0xFF4C4800),
        secondaryFixed = Color(0xFFEAE697),
        secondaryFixedDim = Color(0xFFCECA7E),
        onSecondaryFixed = Color(0xFF1E1C00),
        onSecondaryFixedVariant = Color(0xFF4B490A),
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
