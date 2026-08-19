package app.lineo.converter

import app.lineo.engine.CalcResult
import app.lineo.engine.Engine
import app.lineo.engine.EvalContext
import app.lineo.engine.unit.UnitRegistry
import app.lineo.registry.ModuleRegistry
import app.lineo.registry.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The definition of done of P1-05: ISO 80000 symbols, IEC binary prefixes, and every unit on
 * the screen reachable as text.
 *
 * Everything goes through `Engine.evaluate` with the registry the module contributes, because
 * a catalogue that exists but that no expression can name is the failure this whole task
 * exists to remove.
 */
class ConverterUnitsTest {

    private val units = UnitRegistry.BUILTIN.with(ConverterModule().units())

    private fun convert(source: String): String =
        when (val result = Engine.evaluate(source, EvalContext(locale = Locale.US, units = units))) {
            is CalcResult.Ok -> result.value.canonicalString()
            is CalcResult.Err -> "error: ${result.error}"
        }

    @Test
    fun `every unit the screen offers can be named in an expression`() {
        val unreachable = ConverterCategory.entries
            .flatMap { it.units }
            .filter { unit -> Engine.evaluate("1 ${unit.expression}", context()) !is CalcResult.Ok }
            .map { it.label }

        assertEquals(emptyList<String>(), unreachable)
    }

    @Test
    fun `every category converts its first unit into every other one`() {
        val failures = ConverterCategory.entries.flatMap { category ->
            category.units.drop(1).mapNotNull { target ->
                val source = "1 ${category.defaultFrom.expression} to ${target.expression}"
                val result = Engine.evaluate(source, context())
                if (result is CalcResult.Ok) null else "$source → ${(result as CalcResult.Err).error}"
            }
        }

        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `IEC binary prefixes are powers of two and SI prefixes are powers of ten`() {
        // docs/CONVENTIONS.md §5, and the line of the definition of done that names a number.
        assertEquals("1024 B", convert("1 KiB to B"))
        assertEquals("1000 B", convert("1 kB to B"))
        assertEquals("1048576 B", convert("1 MiB to B"))
        assertEquals("1000000 B", convert("1 MB to B"))
        assertEquals("8 bit", convert("1 B to bit"))
    }

    @Test
    fun `the international definitions are exact`() {
        assertEquals("1852 m", convert("1 nmi to m"))
        assertEquals("0.0002 kg", convert("1 ct to kg"))
        assertEquals("6.35029318 kg", convert("1 st to kg"))
        assertEquals("4046.8564224 m^2", convert("1 acre to m^2"))
        assertEquals("10000 m^2", convert("1 ha to m^2"))
        assertEquals("101325 Pa", convert("1 atm to Pa"))
        assertEquals("3600 J", convert("1 Wh to J"))
        assertEquals("4184 J", convert("1 kcal to J"))
    }

    @Test
    fun `a length is not an area and neither is a mass`() {
        // The dimensions do the refusing, not a category list: `ha` is length squared, so it
        // cannot be read as a length however the screen groups it.
        assertTrue(convert("1 ha to m").startsWith("error:"))
        assertTrue(convert("1 kg to L").startsWith("error:"))
    }

    @Test
    fun `speed and area are expressions rather than invented symbols`() {
        // `m2` would be neither ISO nor lexable, so the screen carries `m^2` and shows `m²`.
        assertEquals("3.6 km/h", convert("1 m/s to km/h"))
        assertEquals("10000 m^2", convert("1 ha to m^2"))
        assertEquals("1.852 km/h", convert("1 kn to km/h"))
    }

    @Test
    fun `temperature comes from the engine and keeps its offset`() {
        // Not registered by this module — °C is affine and `:core:engine` owns it. The
        // category exists so the screen offers it, and the conversion is the engine's.
        assertEquals("212 °F", convert("100 °C to °F"))
        assertEquals("273.15 K", convert("0 °C to K"))
    }

    @Test
    fun `the module registers only what the engine lacks`() {
        val shadowed = ConverterModule().units()
            .flatMap { listOf(it.symbol) + it.aliases }
            .filter { UnitRegistry.BUILTIN.find(it) != null }

        assertEquals(emptyList<String>(), shadowed)
    }

    @Test
    fun `no symbol is registered twice`() {
        val definitions = ConverterModule().units()
        val names = definitions.flatMap { listOf(it.symbol) + it.aliases }

        assertEquals(emptyMap<String, Int>(), names.groupingBy { it }.eachCount().filterValues { it > 1 })
        assertEquals(emptyList<String>(), UnitRegistry.BUILTIN.conflicts(definitions))
    }

    @Test
    fun `the module contributes its catalogue through the registry, and no functions`() {
        val registry = ModuleRegistry(setOf(ConverterModule()))

        assertEquals(ConverterUnits.ALL.size, registry.units(Tier.FREE, Locale.US).size)
        assertEquals(emptyList<Any>(), ConverterModule().functions())
    }

    private fun context() = EvalContext(locale = Locale.US, units = units)
}
