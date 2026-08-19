package app.lineo.engine

import app.lineo.engine.unit.UnitRegistry
import app.lineo.engine.unit.UnitTerm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * The unit rules of `docs/GRAMMAR.md` §3.8 and the precision rule of `AGENTS.md` §2.
 *
 * These same expressions become golden cases once the pipeline can evaluate text (P0-08).
 */
class QuantityArithmeticTest {

    private val span = 0..0

    @Test
    fun `zero point one plus zero point two is exactly zero point three`() {
        val result = QuantityArithmetic.add(number("0.1"), number("0.2"), span)

        assertEquals("0.3", result.canonical())
    }

    @Test
    fun `addition keeps the left operand unit`() {
        val result = QuantityArithmetic.add(quantity("5", "km"), quantity("300", "m"), span)

        assertEquals("5.3 km", result.canonical())
    }

    @Test
    fun `subtraction converts the right operand into the left unit`() {
        val result = QuantityArithmetic.subtract(quantity("5", "km"), quantity("300", "m"), span)

        assertEquals("4.7 km", result.canonical())
    }

    @Test
    fun `incompatible dimensions are a unit mismatch`() {
        val result = QuantityArithmetic.add(quantity("5", "km"), quantity("3", "kg"), span)

        assertTrue(result.errorOrNull() is CalcError.UnitMismatch, result.toString())
    }

    @Test
    fun `a dimensionless value combines freely`() {
        val result = QuantityArithmetic.add(quantity("5", "km"), number("2"), span)

        assertEquals("7 km", result.canonical())
    }

    @Test
    fun `multiplication cancels units symbolically`() {
        val speed = UnitTerm.of(definition("km")) / UnitTerm.of(definition("h"))
        val result = QuantityArithmetic.multiply(
            quantity("2", "h"),
            Quantity(BigDecimal("60"), speed),
            span,
        )

        assertEquals("120 km", result.canonical())
    }

    @Test
    fun `division composes dimensions`() {
        val result = QuantityArithmetic.divide(quantity("100", "m"), quantity("10", "s"), span)

        assertEquals("10 m/s", result.canonical())
    }

    @Test
    fun `division by zero is an error, not an exception`() {
        val result = QuantityArithmetic.divide(number("1"), number("0"), span)

        assertEquals(CalcError.DivisionByZero, result.errorOrNull())
    }

    @Test
    fun `integer powers raise the unit too`() {
        val result = QuantityArithmetic.power(quantity("2", "m"), exponent = 3, span = span)

        assertEquals("8 m^3", result.canonical())
    }

    @Test
    fun `negative powers invert the unit`() {
        val result = QuantityArithmetic.power(quantity("2", "s"), exponent = -1, span = span)

        assertEquals("0.5 1/s", result.canonical())
    }

    @Test
    fun `adding two absolute temperatures is rejected`() {
        val result = QuantityArithmetic.add(quantity("20", "°C"), quantity("5", "°C"), span)

        assertTrue(result.errorOrNull() is CalcError.UnitMismatch, result.toString())
    }

    @Test
    fun `adding a delta to an absolute temperature is allowed`() {
        val result = QuantityArithmetic.add(quantity("20", "°C"), quantity("5", "Δ°C"), span)

        assertEquals("25 °C", result.canonical())
    }

    @Test
    fun `subtracting two absolute temperatures gives a delta`() {
        val result = QuantityArithmetic.subtract(quantity("30", "°C"), quantity("20", "°C"), span)

        assertEquals("10 Δ°C", result.canonical())
    }

    @Test
    fun `multiplying an absolute temperature is rejected`() {
        val result = QuantityArithmetic.multiply(quantity("20", "°C"), number("2"), span)

        assertTrue(result.errorOrNull() is CalcError.UnitMismatch, result.toString())
    }

    @Test
    fun `celsius converts to fahrenheit through kelvin`() {
        val result = QuantityArithmetic.convert(quantity("100", "°C"), unit("°F"), span)

        assertEquals("212 °F", result.canonical())
    }

    @Test
    fun `celsius converts to kelvin`() {
        val result = QuantityArithmetic.convert(quantity("0", "°C"), unit("K"), span)

        assertEquals("273.15 K", result.canonical())
    }

    @Test
    fun `kilometres convert to miles`() {
        val result = QuantityArithmetic.convert(quantity("5", "km"), unit("mi"), span)

        assertTrue(result.canonical().startsWith("3.10685596"), result.canonical())
    }

    @Test
    fun `converting across dimensions is a unit mismatch`() {
        val result = QuantityArithmetic.convert(quantity("5", "km"), unit("kg"), span)

        assertTrue(result.errorOrNull() is CalcError.UnitMismatch, result.toString())
    }

    private fun definition(symbol: String) =
        requireNotNull(UnitRegistry.BUILTIN.find(symbol)) { "unknown unit $symbol" }

    private fun unit(symbol: String) = UnitTerm.of(definition(symbol))

    private fun quantity(value: String, symbol: String) = Quantity(BigDecimal(value), unit(symbol))

    private fun number(value: String) = Quantity(BigDecimal(value))

    private fun CalcResult<Quantity>.canonical(): String =
        valueOrNull()?.canonicalString() ?: "error: ${errorOrNull()}"
}
