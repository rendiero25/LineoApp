package app.lineo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Measures every row of the token mapping in `docs/CONVENTIONS.md` §10 against WCAG 2.1.
 *
 * A palette can be regenerated, a token can be remapped, and a scheme can be added; none of
 * those changes announces itself as an accessibility problem. This test is the thing that
 * notices. It caught the original error row, which put `onErrorContainer` on `error` and
 * measured 1.31:1 in the dark scheme.
 *
 * Only the seeded schemes are measured. The dynamic palettes come from the platform, which
 * guarantees its own contrast — and there is no wallpaper to derive them from here anyway.
 */
class RoleContrastTest {

    private val schemes = listOf(
        "light" to LineoColorSchemes.Light,
        "dark" to LineoColorSchemes.Dark,
        "true black" to LineoColorSchemes.AmoledDark,
    )

    @Test
    fun `every text role meets WCAG AA in every seeded scheme`() {
        val failures = schemes.flatMap { (name, scheme) ->
            textRoles.mapNotNull { role -> measure(name, scheme, role, MINIMUM_TEXT_RATIO) }
        }

        assertTrue(failures.joinToString(separator = "\n", prefix = "\n"), failures.isEmpty())
    }

    @Test
    fun `the error underline meets the WCAG minimum for a non-text mark`() {
        // WCAG 1.4.11: a graphical object that carries meaning needs 3:1, not 4.5:1. The
        // underline is a rule, so it is measured against the editor surface it sits on.
        val failures = schemes.mapNotNull { (name, scheme) ->
            val underline = RoleColors.of(scheme, LineoRole.ErrorUnderline).container
            val editor = RoleColors.of(scheme, LineoRole.Editor).container
            val ratio = contrastRatio(underline, editor)
            if (ratio >= MINIMUM_NON_TEXT_RATIO) null else format(name, "ErrorUnderline", ratio, MINIMUM_NON_TEXT_RATIO)
        }

        assertTrue(failures.joinToString(separator = "\n", prefix = "\n"), failures.isEmpty())
    }

    @Test
    fun `the error underline carries no content colour because it is a shape`() {
        schemes.forEach { (name, scheme) ->
            assertEquals(name, Color.Transparent, RoleColors.of(scheme, LineoRole.ErrorUnderline).content)
        }
    }

    /** Every role that renders text, which is all of them but the underline. */
    private val textRoles: List<LineoRole>
        get() = LineoRole.entries.filterNot { it == LineoRole.ErrorUnderline }

    /** Returns a description of the failure, or null when the pair passes. */
    private fun measure(
        schemeName: String,
        scheme: ColorScheme,
        role: LineoRole,
        minimum: Double,
    ): String? {
        val colors = RoleColors.of(scheme, role)
        // A role with no container of its own is read on the editor behind it.
        val background = if (colors.container == Color.Transparent) {
            RoleColors.of(scheme, LineoRole.Editor).container
        } else {
            colors.container
        }
        val ratio = contrastRatio(colors.content, background)
        return if (ratio >= minimum) null else format(schemeName, role.name, ratio, minimum)
    }

    private fun format(scheme: String, role: String, ratio: Double, minimum: Double): String =
        String.format(Locale.ROOT, "%s / %s: %.2f:1, below the %.1f:1 minimum", scheme, role, ratio, minimum)

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val first = relativeLuminance(foreground)
        val second = relativeLuminance(background)
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }

    private fun relativeLuminance(color: Color): Double =
        0.2126 * linearize(color.red) + 0.7152 * linearize(color.green) + 0.0722 * linearize(color.blue)

    private fun linearize(component: Float): Double {
        val value = component.toDouble()
        return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    private companion object {
        /** WCAG 2.1 AA for text at normal size. */
        const val MINIMUM_TEXT_RATIO = 4.5

        /** WCAG 2.1 AA for a graphical object, criterion 1.4.11. */
        const val MINIMUM_NON_TEXT_RATIO = 3.0
    }
}
