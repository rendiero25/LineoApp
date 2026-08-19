package app.lineo.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import app.lineo.engine.Quantity
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/**
 * A result as the user should read it: the display half of `docs/CONVENTIONS.md` §1.
 *
 * The engine is locale-free by rule — internally a value is a `BigDecimal` and a dot — and
 * this is the one place that turns it into digits with the separators and grouping of a
 * locale. Nothing above the engine may format numbers any other way, and nothing below it
 * may know a locale exists.
 *
 * Grouping comes from the platform's [NumberFormat] rather than from inserting separators by
 * hand, which is what §3 asks for: `hi-IN` groups as `12,34,567`, and that rule is CLDR's to
 * know, not this file's.
 *
 * @param locale the locale to read the number in.
 * @param decimalSeparator overrides the locale's separator, for the Auto / Dot / Comma
 *   setting of §2 that P1-07 owns. The grouping separator follows it — a dot decimal groups
 *   with commas and a comma decimal groups with dots — so the two can never be the same
 *   character.
 */
@Immutable
class QuantityFormat(
    private val locale: Locale,
    private val decimalSeparator: Char? = null,
) {

    private val numbers: DecimalFormat = (NumberFormat.getInstance(locale) as? DecimalFormat ?: DecimalFormat())
        .apply {
            // §3 makes Indian grouping a requirement rather than an enhancement, and the JVM's
            // own data does not supply it for `hi-IN` — it grouped 1234567 as `1,234,567`,
            // which is what this line exists to prevent. The pattern is applied rather than
            // the digits being grouped by hand, so the insertion is still the platform's.
            if (locale.language in INDIAN_GROUPING) applyPattern(INDIAN_PATTERN)
            isGroupingUsed = true
            maximumFractionDigits = MAX_FRACTION_DIGITS
            // HALF_UP, not banker's rounding: §4 calls it out by name, because half-to-even
            // contradicts what anyone outside accounting expects a calculator to do.
            roundingMode = RoundingMode.HALF_UP
            decimalSeparator?.let { separator -> decimalFormatSymbols = symbolsFor(separator) }
        }

    /**
     * Formats [quantity], and says whether anything was cut off.
     *
     * The unit symbol is appended unchanged. Unit symbols are ISO 80000 (§5) and are the same
     * in every locale — translating `km` would be a different unit, not a different spelling.
     */
    fun format(quantity: Quantity): FormattedQuantity {
        val rounded = quantity.value.setScale(MAX_FRACTION_DIGITS, RoundingMode.HALF_UP)
        val digits = numbers.format(quantity.value)
        val unit = quantity.unit?.takeIf { !it.isEmpty }?.symbol
        return FormattedQuantity(
            text = if (unit == null) digits else "$digits $unit",
            truncated = rounded.compareTo(quantity.value) != 0,
        )
    }

    private fun symbolsFor(separator: Char): DecimalFormatSymbols =
        DecimalFormatSymbols.getInstance(locale).apply {
            decimalSeparator = separator
            groupingSeparator = if (separator == ',') '.' else ','
        }

    private companion object {

        /**
         * `1/3` shows as `0.333333333` and says it was cut, per §4. Nine is what that row
         * spells out; it is also about as many digits as anyone reads without counting.
         */
        const val MAX_FRACTION_DIGITS = 9

        /** The languages `docs/CONVENTIONS.md` §3 lists as grouping `12,34,567`. */
        val INDIAN_GROUPING = setOf("hi", "bn", "ta", "te", "mr", "gu", "kn", "ml", "pa")

        /** Two digits per group above the first three — the lakh-crore pattern. */
        const val INDIAN_PATTERN = "#,##,##0"
    }
}

/**
 * A formatted value, and whether the digits shown are all of it.
 *
 * [truncated] is a separate flag rather than a marker inside [text] so that a caller can
 * decide how to show it — and so that a test can tell `0.333333333` the exact value from
 * `0.333333333` the rounded one.
 */
@Immutable
data class FormattedQuantity(val text: String, val truncated: Boolean) {

    /** The value with the truncation mark §4 requires, for a caller that just wants a string. */
    fun display(): String = if (truncated) "$text$TRUNCATION_MARK" else text

    private companion object {
        const val TRUNCATION_MARK = "…"
    }
}

/**
 * The formatter for the current locale.
 *
 * Read from the configuration rather than from `Locale.getDefault()`, so that a locale
 * change recomposes what is on screen instead of waiting for the process to restart.
 */
@Composable
fun rememberQuantityFormat(decimalSeparator: Char? = null): QuantityFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, decimalSeparator) { QuantityFormat(locale, decimalSeparator) }
}
