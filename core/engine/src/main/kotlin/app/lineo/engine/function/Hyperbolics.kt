package app.lineo.engine.function

import app.lineo.engine.CalcResult
import app.lineo.engine.Quantity
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Hyperbolic functions and their inverses.
 *
 * Their argument is a plain number, not an angle — the hyperbolic angle is an area, and no
 * angle mode applies to it — which is why they live apart from [Trigonometry].
 *
 * The JDK ships `sinh`, `cosh` and `tanh` but not the inverses, so those are built from
 * logarithms here, with the squares avoided where they would overflow a `Double` while the
 * function itself is still finite.
 */
internal object Hyperbolics {

    fun sinh(argument: Quantity, span: IntRange): CalcResult<Quantity> =
        Trigonometry.checked(SINH, argument, span, domain = { true }) { kotlin.math.sinh(it) }

    fun cosh(argument: Quantity, span: IntRange): CalcResult<Quantity> =
        Trigonometry.checked(COSH, argument, span, domain = { true }) { kotlin.math.cosh(it) }

    fun tanh(argument: Quantity, span: IntRange): CalcResult<Quantity> =
        Trigonometry.checked(TANH, argument, span, domain = { true }) { kotlin.math.tanh(it) }

    fun asinh(argument: Quantity, span: IntRange): CalcResult<Quantity> =
        Trigonometry.checked(ASINH, argument, span, domain = { true }) { areaSinh(it) }

    /** `cosh` never goes below 1, so neither can its inverse take anything below 1. */
    fun acosh(argument: Quantity, span: IntRange): CalcResult<Quantity> =
        Trigonometry.checked(ACOSH, argument, span, domain = { it >= 1.0 }) { areaCosh(it) }

    /** `tanh` approaches ±1 without reaching it, so ±1 itself is out of range. */
    fun atanh(argument: Quantity, span: IntRange): CalcResult<Quantity> =
        Trigonometry.checked(ATANH, argument, span, domain = { abs(it) < 1.0 }) { areaTanh(it) }

    /** `asinh x = ln(x + √(x²+1))`. */
    private fun areaSinh(x: Double): Double = when {
        abs(x) > LARGE -> (if (x < 0) -1.0 else 1.0) * (ln(abs(x)) + LN_2)
        else -> ln(x + sqrt(x * x + 1.0))
    }

    /** `acosh x = ln(x + √(x²−1))`. */
    private fun areaCosh(x: Double): Double = when {
        x > LARGE -> ln(x) + LN_2
        else -> ln(x + sqrt(x * x - 1.0))
    }

    /** `atanh x = ½ ln((1+x)/(1−x))`. */
    private fun areaTanh(x: Double): Double = HALF * ln((1.0 + x) / (1.0 - x))

    private const val SINH = "sinh"
    private const val COSH = "cosh"
    private const val TANH = "tanh"
    private const val ASINH = "asinh"
    private const val ACOSH = "acosh"
    private const val ATANH = "atanh"

    private const val HALF = 0.5
    private const val LN_2 = 0.6931471805599453

    /** Above this, `x²` overflows a `Double` while the function itself is still finite. */
    private const val LARGE = 1.0e150
}
