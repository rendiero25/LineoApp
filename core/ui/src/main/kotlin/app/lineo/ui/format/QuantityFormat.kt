package app.lineo.ui.format

import androidx.compose.runtime.Immutable
import app.lineo.engine.Quantity
import java.math.BigDecimal
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
 * @param decimalPlaces how many decimals a result may show before it is cut and marked.
 *   A **ceiling**, not a width: `0.5` stays `0.5` rather than being padded, because a
 *   calculator that padded every answer would overstate how precise it was.
 */
@Immutable
class QuantityFormat(
    private val locale: Locale,
    private val decimalSeparator: Char? = null,
    private val decimalPlaces: Int = MAX_FRACTION_DIGITS,
) {

    /** Whether §3's lakh-crore grouping applies, decided once from the language. */
    private val indianGrouping: Boolean = locale.language in INDIAN_GROUPING

    private val numbers: DecimalFormat = (NumberFormat.getInstance(locale) as? DecimalFormat ?: DecimalFormat())
        .apply {
            // §3 makes Indian grouping a requirement rather than an enhancement, and the JVM
            // cannot express it: `java.text.DecimalFormat` keeps a single grouping size, so
            // `#,##,##0` is read as plain groups of three and `hi-IN` prints `1,234,567`.
            // Applying the pattern is therefore not enough — the digits are regrouped in
            // [groupIndian] instead, with the separators CLDR gives for the locale.
            isGroupingUsed = true
            groupingSize = 3
            if (indianGrouping) isGroupingUsed = false

            maximumFractionDigits = decimalPlaces.coerceIn(0, MAX_FRACTION_DIGITS)
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
        val places = decimalPlaces.coerceIn(0, MAX_FRACTION_DIGITS)
        val rounded = quantity.value.setScale(places, RoundingMode.HALF_UP)

        val absValue = quantity.value.abs()
        val useScientific = absValue.signum() != 0 &&
            (absValue >= SCIENTIFIC_UPPER || absValue < SCIENTIFIC_LOWER)

        val digits = if (useScientific) {
            formatScientific(quantity.value)
        } else {
            val formatted = numbers.format(quantity.value)
            if (indianGrouping) groupIndian(formatted) else formatted
        }

        val unit = quantity.unit?.takeIf { !it.isEmpty }?.symbol
        return FormattedQuantity(
            text = if (unit == null) digits else "$digits $unit",
            truncated = rounded.compareTo(quantity.value) != 0,
        )
    }

    /**
     * Renders [value] in scientific notation, e.g., `1.23E15`.
     *
     * Honours the decimal separator and rounding mode. The exponent is always shown with
     * the platform's default exponent symbol ('E').
     */
    private fun formatScientific(value: BigDecimal): String {
        val symbols = numbers.decimalFormatSymbols
        // We use a fixed pattern for the significand but respect the chosen symbols.
        val format = DecimalFormat("0.########E0", symbols)
        format.roundingMode = RoundingMode.HALF_UP
        return format.format(value)
    }

    /**
     * Regroups the integer part of [text] as `12,34,567`: three digits, then pairs.
     *
     * Only the *placement* is ours. Which characters are used is still the locale's — the
     * grouping and decimal separators are read back from the formatter, so an override made
     * in [symbolsFor] is honoured here as well.
     */
    private fun groupIndian(text: String): String {
        val symbols = numbers.decimalFormatSymbols
        val start = text.indexOfFirst(Char::isDigit).takeIf { it >= 0 } ?: return text
        val end = text.indexOf(symbols.decimalSeparator).takeIf { it >= 0 } ?: text.length
        val integer = text.substring(start, end)
        if (integer.length <= FIRST_INDIAN_GROUP) return text

        var index = integer.length - FIRST_INDIAN_GROUP
        val grouped = StringBuilder(integer.substring(index))
        while (index > 0) {
            val from = (index - INDIAN_GROUP).coerceAtLeast(0)
            grouped.insert(0, symbols.groupingSeparator).insert(0, integer.substring(from, index))
            index = from
        }
        return text.substring(0, start) + grouped + text.substring(end)
    }

    private fun symbolsFor(separator: Char): DecimalFormatSymbols =
        DecimalFormatSymbols.getInstance(locale).apply {
            decimalSeparator = separator
            groupingSeparator = if (separator == ',') '.' else ','
        }

    companion object {

        /**
         * `1/3` shows as `0.333333333` and says it was cut, per §4. Nine is what that row
         * spells out; it is also about as many digits as anyone reads without counting.
         *
         * It is also the ceiling on the P1-07 setting: a user may show fewer decimals, never
         * more, because beyond this the digits are the `Double` boundary's noise rather than
         * the answer.
         */
        const val MAX_FRACTION_DIGITS = 9

        /** The languages `docs/CONVENTIONS.md` §3 lists as grouping `12,34,567`. */
        val INDIAN_GROUPING = setOf("hi", "bn", "ta", "te", "mr", "gu", "kn", "ml", "pa")

        /** The lowest group of the lakh-crore pattern — the hundreds, as everywhere else. */
        const val FIRST_INDIAN_GROUP = 3

        /** Every group above it: two digits, so `1234567` reads `12,34,567`. */
        const val INDIAN_GROUP = 2

        /** Numbers larger than this use scientific notation. */
        private val SCIENTIFIC_UPPER = BigDecimal("1000000000000")

        /** Positive numbers smaller than this use scientific notation. */
        private val SCIENTIFIC_LOWER = BigDecimal("0.000001")
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
