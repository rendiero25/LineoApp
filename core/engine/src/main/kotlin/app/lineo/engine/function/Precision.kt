package app.lineo.engine.function

import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.DomainReason
import app.lineo.engine.Quantity
import app.lineo.engine.err
import app.lineo.engine.ok
import app.lineo.engine.unit.UnitTerm
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * The one place where a `Double` becomes a result.
 *
 * `AGENTS.md` §2 forbids `Double` in user-visible arithmetic; `docs/ARCHITECTURE.md` §7
 * carves out transcendental implementations provided the result is re-boxed to
 * `BigDecimal`. Keeping that boundary in a single file is what makes the exception
 * auditable — every trig, log and root result passes through here.
 *
 * A `Double` carries about 15–17 significant decimal digits, and the last two are noise:
 * `sin(30°)` computes as `0.49999999999999994`. Results are therefore rounded to
 * [SIGNIFICANT_DIGITS] with `HALF_UP` (`docs/CONVENTIONS.md` §4 — never banker's rounding),
 * which turns that into `0.5`.
 */
internal object Precision {

    /** Digits kept from a `Double`. Below its ~17, above anything a user would type. */
    const val SIGNIFICANT_DIGITS = 15

    private val CONTEXT = MathContext(SIGNIFICANT_DIGITS, RoundingMode.HALF_UP)

    /**
     * Re-boxes [value] as a dimensionless quantity, mapping the two `Double` states that are
     * not numbers onto the error model: `NaN` means the function was asked for something
     * undefined, an infinity means the result left the representable range.
     */
    fun ofDouble(value: Double, fn: String, span: IntRange, unit: UnitTerm? = null): CalcResult<Quantity> = when {
        value.isNaN() -> CalcError.DomainError(fn, DomainReason.UNDEFINED, span).err()
        value.isInfinite() -> CalcError.Overflow(span).err()
        else -> Quantity(round(value), unit).ok()
    }

    /** [value] at [SIGNIFICANT_DIGITS], with the trailing zeros the rounding introduces removed. */
    fun round(value: Double): BigDecimal = BigDecimal(value, CONTEXT).stripTrailingZeros()

    /**
     * [value] as a `Double`, or `null` when it is too large to be one.
     *
     * The lexer accepts `1e999`, so this is reachable from ordinary input, not just from the
     * fuzz test. Callers that can do better with the magnitude — the logarithms — do.
     */
    fun toFiniteDouble(value: BigDecimal): Double? = value.toDouble().takeIf { it.isFinite() }

    /**
     * Digits before the decimal point. `1e999999999` is cheap to hold but expensive to write
     * out, so anything that would have to materialise those digits — an exact integer, a
     * remainder — checks this first and reports a domain error instead of hanging
     * (`docs/GRAMMAR.md` §6: never throws, never hangs).
     */
    fun integerDigits(value: BigDecimal): Int = value.precision() - value.scale()

    /** Above this many integer digits, exact whole-number work is refused. */
    const val MAX_INTEGER_DIGITS = 1_000
}
