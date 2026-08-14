package app.lineo.engine.function

import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.DomainReason
import app.lineo.engine.EvalContext
import app.lineo.engine.Quantity

/**
 * A function callable from an expression.
 *
 * `:core:registry` exposes the same shape as `CalcFunction` (`docs/ARCHITECTURE.md` §4) so
 * that a module's function is available as text in notepad mode. The engine keeps its own
 * type to stay free of any dependency on the registry module.
 */
data class EngineFunction(
    val name: String,
    val arity: IntRange,
    val aliases: List<String> = emptyList(),
    val evaluate: (arguments: List<Quantity>, context: EvalContext, span: IntRange) -> CalcResult<Quantity>,
) {
    val names: List<String> get() = listOf(name) + aliases

    /** Rejects a call whose argument count is outside [arity]. */
    fun arityError(arguments: List<Quantity>, span: IntRange): CalcError? =
        if (arguments.size in arity) null else CalcError.DomainError(name, DomainReason.OUT_OF_RANGE, span)
}

/** The functions an evaluation can see. Built-ins live in `BuiltinFunctions`. */
class FunctionRegistry(functions: List<EngineFunction> = emptyList()) {

    private val byName: Map<String, EngineFunction> =
        functions.flatMap { function -> function.names.map { it to function } }.toMap()

    val names: Set<String> get() = byName.keys

    fun find(name: String): EngineFunction? = byName[name]

    /** Registry plus the functions contributed by a module (`docs/ARCHITECTURE.md` §4). */
    fun with(extra: List<EngineFunction>): FunctionRegistry =
        FunctionRegistry(byName.values.distinct() + extra)

    companion object {
        val EMPTY = FunctionRegistry()
    }
}
