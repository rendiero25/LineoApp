package app.lineo.ui.format

import app.lineo.engine.MATH_CONTEXT
import app.lineo.engine.Quantity
import app.lineo.engine.unit.UnitFactor
import app.lineo.engine.unit.UnitRegistry
import app.lineo.engine.unit.UnitTerm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

/**
 * The display boundary of `docs/CONVENTIONS.md` §1, in the locales §2 and §3 name.
 *
 * These assert the *output* side only. What a user types is read by the lexer, which has its
 * own locale tests; the two meet in the middle at a `BigDecimal` that carries no separator
 * of any kind.
 */
class QuantityFormatTest {

    @Test
    fun `a dot-decimal locale groups with commas`() {
        assertEquals("1,234,567.89", format(Locale.US, "1234567.89"))
    }

    @Test
    fun `a comma-decimal locale groups with dots`() {
        assertEquals("1.234.567,89", format(Locale.GERMANY, "1234567.89"))
    }

    @Test
    fun `Indian grouping is the platform's, not ours`() {
        assertEquals("12,34,567.89", format(Locale.forLanguageTag("hi-IN"), "1234567.89"))
    }

    @Test
    fun `an Indonesian user reads a rupiah figure the way they wrote it`() {
        assertEquals("8.500.000", format(Locale.forLanguageTag("id-ID"), "8500000"))
    }

    @Test
    fun `the BigDecimal guarantee survives formatting`() {
        assertEquals("0.3", format(Locale.US, "0.3"))
    }

    @Test
    fun `an integer keeps no decimals`() {
        assertEquals("42", format(Locale.US, "42"))
    }

    @Test
    fun `a third is cut at nine places and says so`() {
        val third = BigDecimal.ONE.divide(BigDecimal(3), MATH_CONTEXT)
        val formatted = QuantityFormat(Locale.US).format(Quantity(third))

        assertEquals("0.333333333", formatted.text)
        assertTrue(formatted.truncated)
        assertEquals("0.333333333…", formatted.display())
    }

    @Test
    fun `a value that fits is not marked as cut`() {
        val formatted = QuantityFormat(Locale.US).format(Quantity(BigDecimal("2.5")))

        assertFalse(formatted.truncated)
        assertEquals("2.5", formatted.display())
    }

    @Test
    fun `the separator override flips grouping with it`() {
        val comma = QuantityFormat(Locale.US, decimalSeparator = ',')
        val dot = QuantityFormat(Locale.GERMANY, decimalSeparator = '.')

        assertEquals("1.234.567,89", comma.format(quantity("1234567.89")).text)
        assertEquals("1,234,567.89", dot.format(quantity("1234567.89")).text)
    }

    @Test
    fun `a unit symbol is appended and never translated`() {
        val km = UnitTerm(listOf(UnitFactor(requireNotNull(UnitRegistry.BUILTIN.find("km")), exponent = 1)))
        val quantity = Quantity(BigDecimal("5.3"), km)

        assertEquals("5,3 km", QuantityFormat(Locale.GERMANY).format(quantity).text)
    }

    private fun format(locale: Locale, value: String): String =
        QuantityFormat(locale).format(quantity(value)).text

    private fun quantity(value: String) = Quantity(BigDecimal(value))
}
