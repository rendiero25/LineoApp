package app.lineo.registry

import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.DomainReason
import app.lineo.engine.EvalContext
import app.lineo.engine.Quantity
import app.lineo.engine.function.EngineFunction

/**
 * A function a module contributes to the engine, per `docs/ARCHITECTURE.md` §4.
 *
 * This is the shape a module author writes. The engine has its own
 * [EngineFunction], which carries the call's source span so that built-ins can point at
 * it; a module cannot know that span, so [toEngineFunction] supplies it and rewrites the
 * span of whatever error comes back. That is also why `:core:engine` stays free of any
 * dependency on this module.
 *
 * [evaluate] must not throw. It returns a [CalcResult], because errors here are UI state,
 * never exceptions (`AGENTS.md` §2).
 */
data class CalcFunction(
    val name: String,
    val aliases: List<String> = emptyList(),
    val arity: IntRange,
    val signature: List<ParamSpec> = emptyList(),
    val evaluate: (arguments: List<Quantity>, context: EvalContext) -> CalcResult<Quantity>,
) {
    /** The name plus every alias, which is what the engine registers and the lexer sees. */
    val names: List<String> get() = listOf(name) + aliases
}

/** One declared parameter. Used for signature hints and for the keypad's insert templates. */
data class ParamSpec(
    val name: String,
    val kind: ParamKind = ParamKind.NUMBER,
    val optional: Boolean = false,
)

/** What a parameter accepts. Enforcement is the function's own job; this is a hint. */
enum class ParamKind {
    /** A plain number: a unit is a domain error, as in `ln(5 km)`. */
    NUMBER,

    /** Any quantity, unit or not, as in `abs(-5 km)`. */
    QUANTITY,

    /** An angle: a bare number read in the current angle mode, or a quantity in `°`/`rad`/`grad`. */
    ANGLE,

    /** A whole number, as in `nCr(5, 2)`. */
    INTEGER,
}

/**
 * Adapts a module function to what the evaluator calls.
 *
 * Two things happen here. The argument count is checked once, centrally, so no module has
 * to repeat it; and the error coming back is re-spanned onto the call site, since a module
 * has no view of the source text. An error whose span is already `null` by contract —
 * `DivisionByZero`, `CircularReference`, `RateUnavailable` — keeps it.
 */
internal fun CalcFunction.toEngineFunction(): EngineFunction = EngineFunction(
    name = name,
    arity = arity,
    aliases = aliases,
) { arguments, context, span ->
    if (arguments.size !in arity) {
        CalcResult.Err(CalcError.DomainError(name, DomainReason.OUT_OF_RANGE, span))
    } else {
        when (val result = evaluate(arguments, context)) {
            is CalcResult.Ok -> result
            is CalcResult.Err -> CalcResult.Err(result.error.at(span))
        }
    }
}

/** The same error, pointing at [span]. Variants that never carry a span are returned as they are. */
private fun CalcError.at(span: IntRange): CalcError = when (this) {
    is CalcError.Syntax -> copy(span = span)
    is CalcError.UnbalancedParen -> copy(span = span)
    is CalcError.UnknownIdentifier -> copy(span = span)
    is CalcError.UnitMismatch -> copy(span = span)
    is CalcError.DomainError -> copy(span = span)
    is CalcError.Overflow -> copy(span = span)
    is CalcError.CircularReference,
    is CalcError.RateUnavailable,
    CalcError.DivisionByZero,
    -> this
}
