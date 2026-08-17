package app.lineo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

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

    /**
     * The three styles that carry numbers, sized above the Material scale.
     *
     * `displayLarge` is the top of that scale, and a calculator wanted more: the expression
     * and its result are the only thing on the screen worth reading, and the keys are meant
     * to be hit without looking. These are the Material styles at 1.2×, line height scaled
     * with them so nothing clips.
     *
     * Sizes and not roles, because there is no role left above `displayLarge` — but named
     * here rather than written at the call sites, so `docs/CONVENTIONS.md` §10 still has
     * something to point at and there is one place to change them.
     */
    val Expression: TextStyle = Default.displayLarge.copy(fontSize = 68.sp, lineHeight = 76.sp)

    val Result: TextStyle = Default.displayMedium.copy(fontSize = 54.sp, lineHeight = 62.sp)

    val KeypadLabel: TextStyle = Default.headlineLarge.copy(fontSize = 38.sp, lineHeight = 48.sp)
}
