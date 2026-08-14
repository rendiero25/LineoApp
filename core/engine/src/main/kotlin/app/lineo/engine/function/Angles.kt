package app.lineo.engine.function

import app.lineo.engine.AngleMode
import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.DomainReason
import app.lineo.engine.MATH_CONTEXT
import app.lineo.engine.Quantity
import app.lineo.engine.err
import app.lineo.engine.ok
import app.lineo.engine.unit.UnitRegistry
import java.math.BigDecimal

/**
 * Reads an argument as a plane angle.
 *
 * Two rules, both from `docs/CONVENTIONS.md` §4 (the angle mode is the single most common
 * source of wrong calculator answers, so it is never silent):
 *
 * - A quantity that carries an angle unit means what it says: `sin(90°)` is `1` even in RAD.
 * - A bare number is read in the current [AngleMode].
 *
 * Angles that land exactly on a quarter turn are reported as such rather than as radians.
 * That is what lets `cos(90)` be exactly `0` instead of `6.12e-17`: no `Double` is involved.
 * In RAD only zero is exact, because every other quarter turn is a multiple of an
 * irrational number and cannot be written down exactly.
 */
internal object Angles {

    /** An angle, either exactly on a quarter turn or as radians for the `Double` path. */
    sealed interface Angle {
        @JvmInline
        value class Quarters(val count: Int) : Angle

        @JvmInline
        value class Radians(val value: Double) : Angle
    }

    private const val DEGREE = "°"
    private const val GRADIAN = "grad"
    private const val RADIAN = "rad"
    private const val QUARTERS_PER_TURN = 4

    private val FOUR = BigDecimal(QUARTERS_PER_TURN)
    private val GRADIANS_PER_RADIAN = 200.0 / Math.PI

    /** Quarter turn in each angle unit: 90°, 100 grad. Radians have no exact one. */
    private val QUARTER_TURN: Map<String, BigDecimal> = mapOf(
        DEGREE to BigDecimal("90"),
        GRADIAN to BigDecimal("100"),
    )

    fun of(quantity: Quantity, mode: AngleMode, fn: String, span: IntRange): CalcResult<Angle> {
        val unit = quantity.unit
        val symbol = when {
            unit == null || unit.isEmpty -> symbolOf(mode)
            unit.isAngle -> unit.factors.first().definition.symbol
            // A length or a mass is not an angle, and silently ignoring the unit would be worse.
            else -> return CalcError.DomainError(fn, DomainReason.OUT_OF_RANGE, span).err()
        }

        exactQuarters(quantity.value, symbol)?.let { return Angle.Quarters(it).ok() }

        val scale = requireNotNull(UnitRegistry.find(symbol)) { "unknown angle unit $symbol" }.scale
        val radians = Precision.toFiniteDouble(quantity.value.multiply(scale, MATH_CONTEXT))
            ?: return CalcError.DomainError(fn, DomainReason.OUT_OF_RANGE, span).err()
        return Angle.Radians(radians).ok()
    }

    /** Radians back into [mode], for the inverse functions. */
    fun fromRadians(radians: Double, mode: AngleMode): Double = when (mode) {
        AngleMode.DEG -> Math.toDegrees(radians)
        AngleMode.RAD -> radians
        AngleMode.GRAD -> radians * GRADIANS_PER_RADIAN
    }

    private fun symbolOf(mode: AngleMode): String = when (mode) {
        AngleMode.DEG -> DEGREE
        AngleMode.RAD -> RADIAN
        AngleMode.GRAD -> GRADIAN
    }

    /** How many quarter turns [value] is, counted from zero, or `null` when it is between them. */
    private fun exactQuarters(value: BigDecimal, symbol: String): Int? {
        val quarter = QUARTER_TURN[symbol]
            ?: return if (value.signum() == 0) 0 else null
        // The remainder of 1e999999999 by 90 is a billion digits of division. Not worth it:
        // such an angle has no meaningful sine anyway, and the caller reports out of range.
        if (Precision.integerDigits(value) > Precision.MAX_INTEGER_DIGITS) return null
        if (value.remainder(quarter).signum() != 0) return null

        val quarters = value.divide(quarter, MATH_CONTEXT).remainder(FOUR).toInt()
        return if (quarters < 0) quarters + QUARTERS_PER_TURN else quarters
    }
}
