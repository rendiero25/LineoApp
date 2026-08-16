package app.lineo.registry

import app.lineo.engine.function.FunctionRegistry
import app.lineo.engine.unit.UnitDefinition
import java.util.Locale

/**
 * The modules this build contains, and the bridge from them to the engine
 * (`docs/ARCHITECTURE.md` §4).
 *
 * `:app` builds one of these from the Hilt multibinding and hands the resulting
 * [FunctionRegistry] to every evaluation, so a function registered by
 * `:feature:scientific` is available as text in notepad mode without notepad knowing that
 * module exists.
 *
 * Nothing here is user input, so nothing here can fail at evaluation time: a module that
 * would shadow an existing name is dropped when the registry is built, and [conflicts]
 * reports what was dropped.
 */
class ModuleRegistry(modules: Set<CalculatorModule>) {

    /** Every module in the build, ordered by id. A repeated id keeps the first module only. */
    val all: List<CalculatorModule> = modules.sortedBy { it.id }.distinctBy { it.id }

    /** The modules a user of this [entitlement] in this [locale] may see and call. */
    fun visible(entitlement: Tier, locale: Locale): List<CalculatorModule> = all.filter { module ->
        module.locales.allows(locale) && (entitlement == Tier.PREMIUM || module.tier == Tier.FREE)
    }

    /** The unit symbols contributed by the visible modules, for `:feature:converter` and friends. */
    fun units(entitlement: Tier, locale: Locale): List<UnitDefinition> =
        visible(entitlement, locale).flatMap { it.units() }

    /**
     * [base] plus every function the visible modules contribute.
     *
     * A module cannot replace a name that already exists — a built-in, or a function an
     * earlier module registered. Silently rebinding `sin` from a feature module would make
     * the same expression mean different things in different builds.
     */
    fun functionRegistry(
        entitlement: Tier,
        locale: Locale,
        base: FunctionRegistry = FunctionRegistry.BUILTIN,
    ): FunctionRegistry = base.with(accepted(entitlement, locale, base).map { it.toEngineFunction() })

    /** The names that were dropped as duplicates, so a debug build can surface them. */
    fun conflicts(
        entitlement: Tier,
        locale: Locale,
        base: FunctionRegistry = FunctionRegistry.BUILTIN,
    ): List<String> {
        val taken = base.names.toMutableSet()
        return visible(entitlement, locale)
            .flatMap { it.functions() }
            .flatMap { function ->
                val clashing = function.names.filter { it in taken }
                taken += function.names
                clashing
            }
    }

    private fun accepted(entitlement: Tier, locale: Locale, base: FunctionRegistry): List<CalcFunction> {
        val taken = base.names.toMutableSet()
        return visible(entitlement, locale)
            .flatMap { it.functions() }
            .filter { function ->
                val free = function.names.none { it in taken }
                if (free) taken += function.names
                free
            }
    }
}
