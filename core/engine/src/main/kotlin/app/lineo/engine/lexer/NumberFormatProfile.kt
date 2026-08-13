package app.lineo.engine.lexer

import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * How numbers are written in a given locale (`docs/CONVENTIONS.md` §2, §3, §7).
 *
 * Symbols come from [DecimalFormatSymbols]; nothing here is hardcoded per locale except
 * the magnitude suffix table, which CLDR does not provide.
 */
data class NumberFormatProfile(
    val locale: Locale,
    val decimalSeparator: Char,
    val groupingSeparator: Char,
    val argumentSeparator: Char,
    val groupSizes: Set<Int>,
    val magnitudeSuffixes: Map<String, BigDecimal>,
) {
    /**
     * True when the grouping separator doubles as the argument separator — the case in
     * dot-decimal locales, where `1,234` is grouping but `max(1, 5)` is two arguments.
     */
    val groupingIsAmbiguous: Boolean get() = groupingSeparator == argumentSeparator

    fun isGroupingCharacter(char: Char): Boolean =
        char != decimalSeparator && (char == groupingSeparator || char in NEUTRAL_GROUPING)

    companion object {
        /** Spaces used as grouping separators by several locales, plus their no-break forms. */
        private val NEUTRAL_GROUPING = setOf(' ', ' ', ' ')

        private const val COMMA = ','
        private const val SEMICOLON = ';'

        /** Locales that group by two digits after the first three: 12,34,567. */
        private val INDIAN_GROUPING = setOf("hi", "bn", "ta", "te", "mr", "gu", "kn", "ml", "pa")

        /** Digit counts that make a run of digits a group rather than an argument. */
        private val WESTERN_GROUP_SIZES = setOf(3)
        private val INDIAN_GROUP_SIZES = setOf(2, 3)

        private val THOUSAND = BigDecimal("1000")
        private val MILLION = BigDecimal("1000000")
        private val BILLION = BigDecimal("1000000000")
        private val TRILLION = BigDecimal("1000000000000")
        private val LAKH = BigDecimal("100000")
        private val CRORE = BigDecimal("10000000")

        /** `docs/CONVENTIONS.md` §7. Suffixes are locale-gated, never global. */
        private val SUFFIXES: Map<String, Map<String, BigDecimal>> = mapOf(
            "en" to mapOf("k" to THOUSAND, "m" to MILLION, "b" to BILLION, "t" to TRILLION),
            "id" to mapOf("rb" to THOUSAND, "jt" to MILLION, "m" to BILLION, "t" to TRILLION),
            "hi" to mapOf("k" to THOUSAND, "lakh" to LAKH, "cr" to CRORE),
            "de" to mapOf("mil" to THOUSAND, "mio" to MILLION),
            "es" to mapOf("mil" to THOUSAND, "mio" to MILLION),
            "pt" to mapOf("mil" to THOUSAND, "mio" to MILLION),
        )

        fun forLocale(locale: Locale): NumberFormatProfile {
            val symbols = DecimalFormatSymbols.getInstance(locale)
            val decimal = symbols.decimalSeparator
            return NumberFormatProfile(
                locale = locale,
                decimalSeparator = decimal,
                groupingSeparator = symbols.groupingSeparator,
                // A comma decimal separator forces the argument separator to a semicolon,
                // exactly as spreadsheets do (`docs/CONVENTIONS.md` §2).
                argumentSeparator = if (decimal == COMMA) SEMICOLON else COMMA,
                groupSizes = if (locale.language in INDIAN_GROUPING) INDIAN_GROUP_SIZES else WESTERN_GROUP_SIZES,
                magnitudeSuffixes = SUFFIXES[locale.language].orEmpty(),
            )
        }
    }
}
