package app.lineo.engine.function

import app.lineo.engine.AngleMode
import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.DomainReason
import app.lineo.engine.Quantity
import app.lineo.engine.err
import app.lineo.engine.flatMap
import app.lineo.engine.ok
import java.math.BigDecimal
import kotlin.math.abs

/**
 * Circular trigonometry.
 *
 * The direct functions answer exactly on the quarter turns — `sin(180) = 0`, and `tan(90)`
 * is undefined rather than a very large number — and go through [Precision] everywhere
 * else. The inverse functions return their result in the caller's [AngleMode], which is
 * what makes `asin(0.5)` read `30` in DEG and `100` in GRAD.
 */
internal object Trigonometry {

    fun sin(argument: Quantity, mode: AngleMode, span: IntRange): CalcResult<Quantity> =
        circular(SIN, SINE_QUARTERS, argument, mode, span) { kotlin.math.sin(it) }

    fun cos(argument: Quantity, mode: AngleMode, span: IntRange): CalcResult<Quantity> =
        circular(COS, COSINE_QUARTERS, argument, mode, span) { kotlin.math.cos(it) }

    fun tan(argument: Quantity, mode: AngleMode, span: IntRange): CalcResult<Quantity> =
        circular(TAN, TANGENT_QUARTERS, argument, mode, span) { kotlin.math.tan(it) }

    fun asin(argument: Quantity, mode: AngleMode, span: IntRange): CalcResult<Quantity> =
        inverse(ASIN, argument, mode, span, domain = { abs(it) <= 1.0 }) { kotlin.math.asin(it) }

    fun acos(argument: Quantity, mode: AngleMode, span: IntRange): CalcResult<Quantity> =
        inverse(ACOS, argument, mode, span, domain = { abs(it) <= 1.0 }) { kotlin.math.acos(it) }

    fun atan(argument: Quantity, mode: AngleMode, span: IntRange): CalcResult<Quantity> =
        inverse(ATAN, argument, mode, span, domain = { true }) { kotlin.math.atan(it) }

    /**
     * Reads [argument] as a plain number, rejects it when [domain] says so, and re-boxes
     * whatever [approximate] computes. Shared with [Hyperbolics].
     */
    fun checked(
        fn: String,
        argument: Quantity,
        span: IntRange,
        domain: (Double) -> Boolean,
        approximate: (Double) -> Double,
    ): CalcResult<Quantity> = Arguments.finite(argument, fn, span).flatMap { value ->
        if (domain(value)) {
            Precision.ofDouble(approximate(value), fn, span)
        } else {
            CalcError.DomainError(fn, DomainReason.OUT_OF_RANGE, span).err()
        }
    }

    private fun circular(
        fn: String,
        exact: Array<Int?>,
        argument: Quantity,
        mode: AngleMode,
        span: IntRange,
        approximate: (Double) -> Double,
    ): CalcResult<Quantity> = Angles.of(argument, mode, fn, span).flatMap { angle ->
        when (angle) {
            is Angles.Angle.Quarters -> exact[angle.count]
                ?.let { Quantity(BigDecimal(it)).ok() }
                // tan(90°) has no value, not a very large one: the two one-sided limits differ.
                ?: CalcError.DomainError(fn, DomainReason.UNDEFINED, span).err()

            is Angles.Angle.Radians -> Precision.ofDouble(approximate(angle.value), fn, span)
        }
    }

    private fun inverse(
        fn: String,
        argument: Quantity,
        mode: AngleMode,
        span: IntRange,
        domain: (Double) -> Boolean,
        approximate: (Double) -> Double,
    ): CalcResult<Quantity> = checked(fn, argument, span, domain) { value ->
        Angles.fromRadians(approximate(value), mode)
    }

    private const val SIN = "sin"
    private const val COS = "cos"
    private const val TAN = "tan"
    private const val ASIN = "asin"
    private const val ACOS = "acos"
    private const val ATAN = "atan"

    /** Values on the quarter turns, indexed by quarter. `null` means undefined. */
    private val SINE_QUARTERS = arrayOf<Int?>(0, 1, 0, -1)
    private val COSINE_QUARTERS = arrayOf<Int?>(1, 0, -1, 0)
    private val TANGENT_QUARTERS = arrayOf<Int?>(0, null, 0, null)
}
