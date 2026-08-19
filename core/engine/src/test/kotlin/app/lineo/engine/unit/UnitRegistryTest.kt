package app.lineo.engine.unit

import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.Engine
import app.lineo.engine.EvalContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * What P1-05-0 adds: the units an evaluation can see are given to it, the way its functions
 * already were.
 *
 * The tests go through `Engine.evaluate` rather than through the registry alone, because the
 * thing being added is reachability: a definition that exists but that no expression can name
 * is the failure this exists to prevent — the same one `ModuleRegistry.units()` had, collecting
 * definitions nothing ever read.
 */
class UnitRegistryTest {

    private val byte = UnitDefinition("B", Dimensions.INFORMATION, BigDecimal("8"), aliases = listOf("byte"))
    private val bit = UnitDefinition("bit", Dimensions.INFORMATION, BigDecimal.ONE)
    private val kibibyte = UnitDefinition("KiB", Dimensions.INFORMATION, BigDecimal("8192"))

    private fun contextWith(vararg definitions: UnitDefinition) =
        EvalContext(units = UnitRegistry.BUILTIN.with(definitions.toList()))

    private fun evaluate(source: String, context: EvalContext = contextWith(byte, bit, kibibyte)): String =
        when (val result = Engine.evaluate(source, context)) {
            is CalcResult.Ok -> result.value.canonicalString()
            is CalcResult.Err -> "error: ${result.error}"
        }

    @Test
    fun `a contributed unit is nameable, convertible, and dimensioned`() {
        assertEquals("1024 B", evaluate("1 KiB to B"))
        assertEquals("8192 bit", evaluate("1 KiB to bit"))
        // The alias names the same unit, and the result prints the definition's own symbol.
        assertEquals("1024 B", evaluate("1 KiB to byte"))
    }

    @Test
    fun `IEC binary prefixes are powers of two and SI prefixes are not`() {
        // docs/CONVENTIONS.md §5: KiB is 1024 bytes, kB is 1000. A converter that got this
        // wrong would be wrong by 2.4% and nobody would see it.
        val kilobyte = UnitDefinition("kB", Dimensions.INFORMATION, BigDecimal("8000"))

        assertEquals("1000 B", evaluate("1 kB to B", contextWith(byte, kilobyte)))
        assertEquals("1024 B", evaluate("1 KiB to B", contextWith(byte, kibibyte)))
    }

    @Test
    fun `information is a dimension of its own`() {
        // Data is not a length and not a mass, which is the whole reason `Dimensions.information`
        // exists — without it every byte would be dimensionless and this would be accepted.
        // A bare number still combines freely, per the golden file: `5 km + 2` is `7 km`.
        val error = (Engine.evaluate("1 KiB + 3 kg", contextWith(byte, kibibyte)) as CalcResult.Err).error

        assertTrue(error is CalcError.UnitMismatch, error.toString())
        assertEquals("4 KiB", evaluate("1 KiB + 3", contextWith(byte, kibibyte)))
    }

    @Test
    fun `data over time is a rate rather than an error`() {
        // Parenthesised, because implicit multiplication sits at the same level as `/`
        // (docs/GRAMMAR.md §2: `6/2(1+3)` is `12`), so `1 KiB / 2 s` is `(1 KiB / 2) × s`.
        assertEquals("0.5 KiB/s", evaluate("1 KiB / (2 s)"))
    }

    @Test
    fun `without the registry the same symbol is an unknown name`() {
        // The failure this plumbing prevents: a module's catalogue collected and dropped.
        val error = (Engine.evaluate("1 KiB to B", EvalContext()) as CalcResult.Err).error

        assertTrue(error is CalcError.UnknownIdentifier, error.toString())
    }

    @Test
    fun `a contributed unit cannot replace one the engine already has`() {
        // Same rule as a module function shadowing a built-in: silently rebinding `m` would
        // make the same expression mean different things in different builds.
        val fakeMetre = UnitDefinition("m", Dimensions.MASS, BigDecimal("1000"))
        val registry = UnitRegistry.BUILTIN.with(listOf(fakeMetre))

        assertEquals(Dimensions.LENGTH, registry.find("m")?.dimensions)
        assertEquals(listOf("m"), registry.conflicts(listOf(fakeMetre)))
    }

    @Test
    fun `the built-in set is what the grammar itself names`() {
        val builtin = UnitRegistry.BUILTIN

        // Angles and temperatures, because trigonometry and §3.8 refer to them by name.
        assertEquals(UnitKind.ANGLE, builtin.find("°")?.kind)
        assertEquals(UnitKind.ABSOLUTE_TEMPERATURE, builtin.find("°C")?.kind)
        assertEquals("Δ°C", builtin.deltaOf(requireNotNull(builtin.find("°C")))?.symbol)
        // And nothing from the converter's catalogue.
        assertNull(builtin.find("KiB"))
        assertNull(builtin.find("L"))
    }
}
