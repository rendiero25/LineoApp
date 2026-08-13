package app.lineo.engine

import app.lineo.engine.unit.UnitTerm

/**
 * Everything that can go wrong while evaluating user input, per `docs/ARCHITECTURE.md` §3.
 *
 * The engine never throws for bad input: these are UI state. [span] points at the offending
 * characters of the source line so the editor can underline exactly that range.
 */
sealed interface CalcError {
    val span: IntRange?

    data class Syntax(val token: String, override val span: IntRange) : CalcError

    data class UnbalancedParen(val missing: Int, override val span: IntRange?) : CalcError

    data class UnknownIdentifier(
        val name: String,
        val suggestion: String?,
        override val span: IntRange,
    ) : CalcError

    data class UnitMismatch(
        val left: UnitTerm,
        val right: UnitTerm,
        override val span: IntRange,
    ) : CalcError

    data class DomainError(
        val fn: String,
        val reason: DomainReason,
        override val span: IntRange,
    ) : CalcError

    data class CircularReference(val chain: List<LineId>) : CalcError {
        override val span: IntRange? get() = null
    }

    data object DivisionByZero : CalcError {
        override val span: IntRange? get() = null
    }

    data class Overflow(override val span: IntRange?) : CalcError

    data class RateUnavailable(val from: String, val to: String) : CalcError {
        override val span: IntRange? get() = null
    }
}

/** Why a function rejected its arguments. Carried by [CalcError.DomainError]. */
enum class DomainReason {
    NEGATIVE_INPUT,
    ZERO_INPUT,
    OUT_OF_RANGE,
    NON_INTEGER,
    TOO_LARGE,
    UNDEFINED,
}

/**
 * Stable identity of a notepad line. References bind to this, never to the display
 * ordinal (`docs/ARCHITECTURE.md` §6, `docs/GRAMMAR.md` §3.7).
 */
@JvmInline
value class LineId(val value: Long)
