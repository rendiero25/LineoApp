package app.lineo.converter

import androidx.compose.runtime.Composable
import app.lineo.engine.unit.UnitDefinition
import app.lineo.registry.CalcFunction
import app.lineo.registry.CalculatorModule
import app.lineo.registry.LocaleGate
import app.lineo.registry.ModuleNav
import app.lineo.registry.Tier

/**
 * Unit conversion, as a module (`docs/ARCHITECTURE.md` §4).
 *
 * Its contribution is a catalogue rather than functions: `to` is grammar, not a call, so what
 * makes `5 km to mi` work anywhere in the app is [units] reaching the engine's registry. The
 * screen is the same capability with a picker around it — it composes an expression and hands
 * it to the engine, so anything convertible here is convertible by typing.
 *
 * Free and offered everywhere: `docs/SPEC.md` §4 puts unit conversion, all categories, in the
 * free tier.
 */
class ConverterModule : CalculatorModule {

    override val id: String = "converter"

    override val titleRes: Int = R.string.converter_title

    override val iconRes: Int = R.drawable.ic_converter

    override val tier: Tier = Tier.FREE

    override val locales: LocaleGate = LocaleGate.All

    /**
     * None.
     *
     * A converter has nothing to add that is a function: conversion is an operator the grammar
     * already has (`docs/GRAMMAR.md` §3.8), and a `convert(x, from, to)` taking unit names as
     * arguments would be a second, worse spelling of `to`.
     */
    override fun functions(): List<CalcFunction> = emptyList()

    override fun units(): List<UnitDefinition> = ConverterUnits.ALL

    /** [nav] is unused: the screen has nowhere to go, and back belongs to whatever placed it. */
    @Composable
    override fun Screen(nav: ModuleNav) {
        ConverterScreen()
    }
}
