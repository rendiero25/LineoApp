package app.lineo.registry

import androidx.compose.runtime.Composable
import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.DomainReason
import app.lineo.engine.Engine
import app.lineo.engine.EvalContext
import app.lineo.engine.Quantity
import app.lineo.engine.unit.UnitDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

/**
 * The definition of done of P0-10: a module registers a function and the engine evaluates
 * it by name, through nothing but the registry.
 */
class ModuleRegistryTest {

    @Test
    fun `a registered module function is callable by name`() {
        val registry = ModuleRegistry(setOf(DummyModule()))

        val result = Engine.evaluate("double(21)", contextOf(registry))

        assertEquals("42", (result as CalcResult.Ok).value.canonicalString())
    }

    @Test
    fun `a registered function composes with the rest of an expression`() {
        val registry = ModuleRegistry(setOf(DummyModule()))

        val result = Engine.evaluate("double(2) + double(3) * 2", contextOf(registry))

        assertEquals("16", (result as CalcResult.Ok).value.canonicalString())
    }

    @Test
    fun `an alias resolves to the same function`() {
        val registry = ModuleRegistry(setOf(DummyModule()))

        val result = Engine.evaluate("twice(4)", contextOf(registry))

        assertEquals("8", (result as CalcResult.Ok).value.canonicalString())
    }

    @Test
    fun `a premium function is unknown to a free entitlement`() {
        val registry = ModuleRegistry(setOf(PremiumModule()))

        val result = Engine.evaluate("secret(1)", contextOf(registry, entitlement = Tier.FREE))

        assertTrue((result as CalcResult.Err).error is CalcError.UnknownIdentifier)
    }

    @Test
    fun `a locale-gated module is unknown outside its locales`() {
        val registry = ModuleRegistry(setOf(RegionalModule()))

        val elsewhere = Engine.evaluate("ppn(100)", contextOf(registry, locale = Locale.US))
        val inIndonesia = Engine.evaluate("ppn(100)", contextOf(registry, locale = Locale.forLanguageTag("id-ID")))

        assertTrue((elsewhere as CalcResult.Err).error is CalcError.UnknownIdentifier)
        assertEquals("11", (inIndonesia as CalcResult.Ok).value.canonicalString())
    }

    @Test
    fun `a module cannot shadow a built-in`() {
        val registry = ModuleRegistry(setOf(ShadowingModule()))

        val result = Engine.evaluate("sin(0)", contextOf(registry))

        assertEquals("0", (result as CalcResult.Ok).value.canonicalString())
        assertEquals(listOf("sin"), registry.conflicts(Tier.PREMIUM, Locale.US))
    }

    @Test
    fun `a wrong argument count is a domain error at the call site`() {
        val registry = ModuleRegistry(setOf(DummyModule()))

        val result = Engine.evaluate("double(1, 2)", contextOf(registry))

        val error = (result as CalcResult.Err).error as CalcError.DomainError
        assertEquals("double", error.fn)
        assertEquals(DomainReason.OUT_OF_RANGE, error.reason)
        assertEquals(0..11, error.span)
    }

    @Test
    fun `an error returned by a module points at the call site`() {
        val registry = ModuleRegistry(setOf(DummyModule()))

        val result = Engine.evaluate("1 + failing(2)", contextOf(registry))

        val error = (result as CalcResult.Err).error as CalcError.DomainError
        assertEquals(4..13, error.span)
    }

    @Test
    fun `units contributed by a module are collected`() {
        val registry = ModuleRegistry(setOf(RegionalModule()))

        assertEquals(emptyList<UnitDefinition>(), registry.units(Tier.PREMIUM, Locale.US))
        assertEquals(1, registry.units(Tier.PREMIUM, Locale.forLanguageTag("id-ID")).size)
    }

    @Test
    fun `a repeated module id keeps one module only`() {
        val registry = ModuleRegistry(setOf(DummyModule(), DummyModule(name = "triple")))

        assertEquals(1, registry.all.size)
        assertNull(registry.functionRegistry(Tier.PREMIUM, Locale.US).find("triple"))
    }

    @Test
    fun `an empty registry evaluates the built-ins unchanged`() {
        val registry = ModuleRegistry(emptySet())

        val result = Engine.evaluate("sqrt(9)", contextOf(registry))

        assertEquals("3", (result as CalcResult.Ok).value.canonicalString())
    }

    private fun contextOf(
        registry: ModuleRegistry,
        entitlement: Tier = Tier.PREMIUM,
        locale: Locale = Locale.US,
    ) = EvalContext(locale = locale, functions = registry.functionRegistry(entitlement, locale))
}

/** A module that exists only to prove the wiring: one function, one alias, one failure. */
private class DummyModule(private val name: String = "double") : CalculatorModule {
    override val id: String = "dummy"
    override val titleRes: Int = 0
    override val iconRes: Int = 0
    override val tier: Tier = Tier.FREE
    override val locales: LocaleGate = LocaleGate.All

    override fun functions(): List<CalcFunction> = listOf(
        CalcFunction(
            name = name,
            aliases = listOf("twice"),
            arity = 1..1,
            signature = listOf(ParamSpec("x", ParamKind.QUANTITY)),
        ) { arguments, _ ->
            CalcResult.Ok(arguments.first().copy(value = arguments.first().value * BigDecimal("2")))
        },
        CalcFunction(name = "failing", arity = 1..1) { _, _ ->
            // Span 0..0 on purpose: a module has no view of the source, and the registry
            // must replace whatever it invents with the real call site.
            CalcResult.Err(CalcError.DomainError("failing", DomainReason.UNDEFINED, 0..0))
        },
    )

    @Composable
    override fun Screen(nav: ModuleNav) = Unit
}

private class PremiumModule : CalculatorModule {
    override val id: String = "premium"
    override val titleRes: Int = 0
    override val iconRes: Int = 0
    override val tier: Tier = Tier.PREMIUM
    override val locales: LocaleGate = LocaleGate.All

    override fun functions(): List<CalcFunction> = listOf(
        CalcFunction(name = "secret", arity = 1..1) { arguments, _ -> CalcResult.Ok(arguments.first()) },
    )

    @Composable
    override fun Screen(nav: ModuleNav) = Unit
}

/** Stands in for a `:pack:tax-*` module: Indonesian VAT, offered in Indonesia only. */
private class RegionalModule : CalculatorModule {
    override val id: String = "tax-id"
    override val titleRes: Int = 0
    override val iconRes: Int = 0
    override val tier: Tier = Tier.FREE
    override val locales: LocaleGate = LocaleGate.Only(setOf("id"))

    override fun functions(): List<CalcFunction> = listOf(
        CalcFunction(name = "ppn", arity = 1..1) { arguments, _ ->
            CalcResult.Ok(arguments.first().copy(value = arguments.first().value * BigDecimal("0.11")))
        },
    )

    override fun units(): List<UnitDefinition> = listOf(
        UnitDefinition("Rp", app.lineo.engine.unit.Dimensions.NONE, BigDecimal.ONE),
    )

    @Composable
    override fun Screen(nav: ModuleNav) = Unit
}

private class ShadowingModule : CalculatorModule {
    override val id: String = "shadow"
    override val titleRes: Int = 0
    override val iconRes: Int = 0
    override val tier: Tier = Tier.FREE
    override val locales: LocaleGate = LocaleGate.All

    override fun functions(): List<CalcFunction> = listOf(
        CalcFunction(name = "sin", arity = 1..1) { _, _ -> CalcResult.Ok(Quantity.of("999")) },
    )

    @Composable
    override fun Screen(nav: ModuleNav) = Unit
}
