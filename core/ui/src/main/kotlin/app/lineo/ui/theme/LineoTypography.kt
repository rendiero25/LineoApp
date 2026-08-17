package app.lineo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

/**
 * Lineo's type scale: the Material 3 scale with tabular figures switched on.
 *
 * `docs/CONVENTIONS.md` §10 makes tabular figures mandatory wherever a number appears.
 * With proportional figures a `1` is narrower than a `8`, so digits shift sideways as the
 * result updates and the number becomes hard to read while typing — the app looks broken
 * even though the arithmetic is right. Every style carries the feature rather than a
 * chosen few, because a number can appear in any of them: a result, a line label, a unit
 * suffix, an error position.
 *
 * Role assignment lives in §10 as well: `displayMedium` is the expression being typed,
 * `displaySmall` its result, `bodyLarge` a notepad line, `titleMedium` a keypad label,
 * and `bodySmall` an error message.
 */
object LineoTypography {

    /**
     * OpenType feature for tabular (fixed-advance) figures. Every digit then occupies the
     * same width, so columns of numbers line up and a changing digit does not move its
     * neighbours.
     */
    private const val TABULAR_FIGURES = "tnum"

    val Default: Typography = Typography().let { base ->
        base.copy(
            displayLarge = base.displayLarge.tabular(),
            displayMedium = base.displayMedium.tabular(),
            displaySmall = base.displaySmall.tabular(),
            headlineLarge = base.headlineLarge.tabular(),
            headlineMedium = base.headlineMedium.tabular(),
            headlineSmall = base.headlineSmall.tabular(),
            titleLarge = base.titleLarge.tabular(),
            titleMedium = base.titleMedium.tabular(),
            titleSmall = base.titleSmall.tabular(),
            bodyLarge = base.bodyLarge.tabular(),
            bodyMedium = base.bodyMedium.tabular(),
            bodySmall = base.bodySmall.tabular(),
            labelLarge = base.labelLarge.tabular(),
            labelMedium = base.labelMedium.tabular(),
            labelSmall = base.labelSmall.tabular(),
        )
    }

    private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TABULAR_FIGURES)
}
