package app.lineo.engine.function

import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.DomainReason
import app.lineo.engine.Quantity
import app.lineo.engine.err
import app.lineo.engine.flatMap
import app.lineo.engine.ok
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Argument checks shared by the built-ins.
 *
 * Every rejection is a [CalcError.DomainError] carrying the function name and a
 * [DomainReason], so the editor can phrase the message without knowing which function
 * produced it (`docs/ARCHITECTURE.md` §3).
 */
internal object Arguments {

    /** [quantity] as a plain number. A unit is a domain error: `ln(5 km)` has no meaning. */
    fun number(quantity: Quantity, fn: String, span: IntRange): CalcResult<BigDecimal> =
        if (quantity.isDimensionless) {
            quantity.value.ok()
        } else {
            CalcError.DomainError(fn, DomainReason.OUT_OF_RANGE, span).err()
        }

    /** [quantity] as a `Double`, for the transcendental path. Too large to be one is out of range. */
    fun finite(quantity: Quantity, fn: String, span: IntRange): CalcResult<Double> =
        number(quantity, fn, span).flatMap { value ->
            Precision.toFiniteDouble(value)?.ok()
                ?: CalcError.DomainError(fn, DomainReason.OUT_OF_RANGE, span).err()
        }

    /** [quantity] as a whole number. `gcd(2.5, 5)` and `nCr(5, 2.5)` are domain errors. */
    fun integer(quantity: Quantity, fn: String, span: IntRange): CalcResult<BigInteger> =
        number(quantity, fn, span).flatMap { value ->
            when {
                // Writing out 1e999999999 as an integer is a billion digits of work.
                Precision.integerDigits(value) > Precision.MAX_INTEGER_DIGITS ->
                    CalcError.DomainError(fn, DomainReason.TOO_LARGE, span).err()

                else -> value.toBigIntegerOrNull()?.ok()
                    ?: CalcError.DomainError(fn, DomainReason.NON_INTEGER, span).err()
            }
        }

    /** [quantity] as a whole number no larger than [limit], for counts and decimal places. */
    fun count(quantity: Quantity, fn: String, span: IntRange, limit: Int): CalcResult<Int> =
        integer(quantity, fn, span).flatMap { value ->
            if (value.abs() > BigInteger.valueOf(limit.toLong())) {
                CalcError.DomainError(fn, DomainReason.TOO_LARGE, span).err()
            } else {
                value.toInt().ok()
            }
        }

    /** The exact integer [BigDecimal] denotes, or `null` when it has a fractional part. */
    fun BigDecimal.toBigIntegerOrNull(): BigInteger? = try {
        toBigIntegerExact()
    } catch (_: ArithmeticException) {
        null
    }
}
