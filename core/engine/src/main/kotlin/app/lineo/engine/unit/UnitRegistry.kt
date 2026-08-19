package app.lineo.engine.unit

/**
 * The units an evaluation can see.
 *
 * An instance rather than a singleton, and the same shape as `FunctionRegistry`: `:core:engine`
 * ships [BUILTIN] — the handful the grammar itself needs — and a module adds its catalogue with
 * [with], which is how `:feature:converter` makes `5 km to mi` work in notepad mode without the
 * notepad knowing that module exists (`docs/ARCHITECTURE.md` §4).
 *
 * Symbols follow ISO 80000 / SI (`docs/CONVENTIONS.md` §5), so `kg` not `Kg` and `s` not `sec`.
 *
 * A definition whose symbol is already taken is **ignored**, the same rule `ModuleRegistry`
 * applies to functions: silently rebinding `m` from a feature module would make the same
 * expression mean different things in different builds.
 */
class UnitRegistry(definitions: List<UnitDefinition>) {

    private val bySymbol: Map<String, UnitDefinition> = buildMap {
        definitions.forEach { definition ->
            putIfAbsent(definition.symbol, definition)
            definition.aliases.forEach { alias -> putIfAbsent(alias, definition) }
        }
    }

    /** All known symbols, including aliases. Used by the evaluator and by suggestions. */
    val symbols: Set<String> get() = bySymbol.keys

    fun find(symbol: String): UnitDefinition? = bySymbol[symbol]

    /**
     * The delta counterpart of an absolute temperature, e.g. `°C` → `Δ°C`.
     *
     * Looked up by name rather than held on the definition, so a module contributing an
     * absolute temperature contributes its delta the same way — as another symbol.
     */
    fun deltaOf(definition: UnitDefinition): UnitDefinition? =
        if (definition.isAbsoluteTemperature) bySymbol["Δ${definition.symbol}"] else null

    /** This registry plus [extra]. Symbols already here win, and the newcomer is dropped. */
    fun with(extra: List<UnitDefinition>): UnitRegistry =
        UnitRegistry(bySymbol.values.distinct() + extra)

    /** The symbols in [extra] this registry would refuse, so a debug build can surface them. */
    fun conflicts(extra: List<UnitDefinition>): List<String> {
        val taken = symbols.toMutableSet()
        return extra.flatMap { definition ->
            val names = listOf(definition.symbol) + definition.aliases
            val clashing = names.filter { it in taken }
            taken += names
            clashing
        }
    }

    companion object {

        /**
         * What the engine knows without any module: the units the grammar itself refers to.
         *
         * Deliberately a core set — angles, because trigonometry reads them; temperatures,
         * because they are affine and `docs/GRAMMAR.md` §3.8 is about them; and the everyday
         * length, mass and time symbols a bare expression is expected to understand. The full
         * catalogue belongs to `:feature:converter` (P1-05).
         */
        val BUILTIN: UnitRegistry = UnitRegistry(CoreUnits.ALL)
    }
}
