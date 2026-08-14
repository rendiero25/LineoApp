package app.lineo.engine.function

import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.DomainReason
import app.lineo.engine.MATH_CONTEXT
import app.lineo.engine.Quantity
import app.lineo.engine.err
import app.lineo.engine.flatMap
import app.lineo.engine.ok
import java.math.BigDecimal
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/**
 * Logarithms, the exponential, and roots.
 *
 * `Double` covers roughly `1e-308 … 1e308`, but the lexer happily accepts `1e999`
 * (`docs/GRAMMAR.md` §3.4), so these functions work on the decimal magnitude rather than on
 * the value itself: a number is split into a mantissa in `[1, 10)` and a power of ten, the
 * `Double` maths runs on the mantissa, and the power of ten is put back afterwards. That
 * keeps `ln(1e400)` and `sqrt(1e600)` answerable instead of overflowing.
 */
internal object Exponentials {

    fun naturalLog(argument: Quantity, span: IntRange): CalcResult<Quantity> =
        logarithm(LN, argument, span) { it }

    fun log10(argument: Quantity, span: IntRange): CalcResult<Quantity> =
        logarithm(LOG, argument, span) { it / LN_10 }

    fun log2(argument: Quantity, span: IntRange): CalcResult<Quantity> =
        logarithm(LOG2, argument, span) { it / LN_2 }

    /** `log(x, b)` is `ln x / ln b`. Base 1 has no logarithm: every power of it is 1. */
    fun logBase(argument: Quantity, base: Quantity, span: IntRange): CalcResult<Quantity> =
        positive(LOG, base, span).flatMap { baseValue ->
            val divisor = naturalLogOf(baseValue)
            if (divisor == 0.0) {
                CalcError.DomainError(LOG, DomainReason.UNDEFINED, span).err()
            } else {
                logarithm(LOG, argument, span) { it / divisor }
            }
        }

    fun exponential(argument: Quantity, span: IntRange): CalcResult<Quantity> =
        Arguments.finite(argument, EXP, span).flatMap { value ->
            Precision.ofDouble(exp(value), EXP, span)
        }

    fun squareRoot(argument: Quantity, span: IntRange): CalcResult<Quantity> =
        Arguments.number(argument, SQRT, span).flatMap { value -> root(SQRT, value, SQUARE, span) }

    fun cubeRoot(argument: Quantity, span: IntRange): CalcResult<Quantity> =
        Arguments.number(argument, CBRT, span).flatMap { value -> root(CBRT, value, CUBE, span) }

    /** `root(x, n)` is the nth root. The degree is a whole number; `x^(1/2.5)` is what `^` is for. */
    fun nthRoot(argument: Quantity, degree: Quantity, span: IntRange): CalcResult<Quantity> =
        Arguments.count(degree, ROOT, span, MAX_DEGREE).flatMap { n ->
            Arguments.number(argument, ROOT, span).flatMap { value -> root(ROOT, value, n, span) }
        }

    private fun logarithm(
        fn: String,
        argument: Quantity,
        span: IntRange,
        scale: (Double) -> Double,
    ): CalcResult<Quantity> = positive(fn, argument, span).flatMap { value ->
        Precision.ofDouble(scale(naturalLogOf(value)), fn, span)
    }

    /** A logarithm needs a strictly positive argument; zero and negative differ in message. */
    private fun positive(fn: String, argument: Quantity, span: IntRange): CalcResult<BigDecimal> =
        Arguments.number(argument, fn, span).flatMap { value ->
            when (value.signum()) {
                0 -> CalcError.DomainError(fn, DomainReason.ZERO_INPUT, span).err()
                -1 -> CalcError.DomainError(fn, DomainReason.NEGATIVE_INPUT, span).err()
                else -> value.ok()
            }
        }

    /** `ln(m × 10^e) = ln m + e ln 10`, so the magnitude never reaches a `Double`. */
    private fun naturalLogOf(value: BigDecimal): Double {
        val exponent = magnitudeOf(value)
        val mantissa = value.scaleByPowerOfTen(-exponent).toDouble()
        return ln(mantissa) + exponent * LN_10
    }

    private fun root(fn: String, value: BigDecimal, degree: Int, span: IntRange): CalcResult<Quantity> {
        val negative = value.signum() < 0
        return when {
            degree == 0 -> CalcError.DomainError(fn, DomainReason.UNDEFINED, span).err()
            value.signum() == 0 && degree < 0 -> CalcError.DomainError(fn, DomainReason.ZERO_INPUT, span).err()
            // An even root of a negative number is not real; an odd one is: cbrt(-8) is -2.
            negative && degree % 2 == 0 -> CalcError.DomainError(fn, DomainReason.NEGATIVE_INPUT, span).err()
            else -> rootOfMagnitude(value.abs(), degree, negative).ok()
        }
    }

    /**
     * The root of `m × 10^e`, taken as `root(m × 10^(e mod n)) × 10^(e div n)` so that the
     * exponent stays exact and only the mantissa goes through `Double`.
     */
    private fun rootOfMagnitude(value: BigDecimal, degree: Int, negative: Boolean): Quantity {
        if (value.signum() == 0) return Quantity(BigDecimal.ZERO)

        val n = kotlin.math.abs(degree)
        val exponent = magnitudeOf(value)
        val aligned = exponent - Math.floorMod(exponent, n)
        // scaleByPowerOfTen, not movePointLeft/Right: it shifts the scale without ever
        // writing out the digits, which `1e999999999` would otherwise force it to.
        val mantissa = value.scaleByPowerOfTen(-aligned).toDouble()

        val rooted = Precision.round(mantissa.pow(1.0 / n)).scaleByPowerOfTen(aligned / n)
        val signed = if (negative) rooted.negate() else rooted
        return Quantity(if (degree < 0) BigDecimal.ONE.divide(signed, MATH_CONTEXT) else signed)
    }

    /** `floor(log10(value))` for a non-zero value, computed on the decimal representation. */
    private fun magnitudeOf(value: BigDecimal): Int = value.precision() - value.scale() - 1

    private const val LN = "ln"
    private const val LOG = "log"
    private const val LOG2 = "log2"
    private const val EXP = "exp"
    private const val SQRT = "sqrt"
    private const val CBRT = "cbrt"
    private const val ROOT = "root"

    private const val SQUARE = 2
    private const val CUBE = 3

    /** Beyond this the mantissa of an nth root no longer fits a `Double`. */
    private const val MAX_DEGREE = 100

    private val LN_10 = ln(10.0)
    private val LN_2 = ln(2.0)
}
