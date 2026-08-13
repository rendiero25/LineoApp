package app.lineo.engine

import app.lineo.engine.unit.UnitRegistry
import app.lineo.engine.unit.UnitTerm
import java.math.BigDecimal
import java.math.MathContext

/** Internal precision, locked by `AGENTS.md` §2. Never `Double` in a result path. */
val MATH_CONTEXT: MathContext = MathContext.DECIMAL128

/**
 * Unit-aware arithmetic, per `docs/GRAMMAR.md` §3.8.
 *
 * - Addition and subtraction need compatible dimensions and keep the **left** operand's
 *   unit: `5 km + 300 m` is `5.3 km`.
 * - Multiplication and division compose dimensions symbolically, so `2 h * 60 km/h` is
 *   `120 km` — the `h` cancels rather than being converted.
 * - A dimensionless value combines freely with any quantity.
 * - Absolute temperatures are affine: adding two of them is rejected, adding a delta is
 *   not, and subtracting two yields a delta.
 */
object QuantityArithmetic {

    fun add(left: Quantity, right: Quantity, span: IntRange): CalcResult<Quantity> =
        combineAdditive(left, right, span, subtract = false)

    fun subtract(left: Quantity, right: Quantity, span: IntRange): CalcResult<Quantity> =
        combineAdditive(left, right, span, subtract = true)

    fun multiply(left: Quantity, right: Quantity, span: IntRange): CalcResult<Quantity> {
        rejectAffine(left, right, span)?.let { return it }
        val unit = unitOf(left) * unitOf(right)
        return Quantity(left.value.multiply(right.value, MATH_CONTEXT), unit.orNull()).ok()
    }

    fun divide(left: Quantity, right: Quantity, span: IntRange): CalcResult<Quantity> {
        if (right.value.signum() == 0) return CalcError.DivisionByZero.err()
        rejectAffine(left, right, span)?.let { return it }
        val unit = unitOf(left) / unitOf(right)
        return Quantity(left.value.divide(right.value, MATH_CONTEXT), unit.orNull()).ok()
    }

    /** Integer power. Fractional powers of a united quantity are rejected by the evaluator. */
    fun power(base: Quantity, exponent: Int, span: IntRange): CalcResult<Quantity> {
        rejectAffine(base, base, span)?.let { return it }
        val value = if (exponent >= 0) {
            base.value.pow(exponent, MATH_CONTEXT)
        } else {
            BigDecimal.ONE.divide(base.value.pow(-exponent, MATH_CONTEXT), MATH_CONTEXT)
        }
        return Quantity(value, unitOf(base).pow(exponent).orNull()).ok()
    }

    fun negate(quantity: Quantity): Quantity = quantity.copy(value = quantity.value.negate())

    /**
     * Converts [quantity] into [target]. Dimensions must match; affine units go through
     * their base (kelvin) so `°C → °F` is not a bare scale factor.
     */
    fun convert(quantity: Quantity, target: UnitTerm, span: IntRange): CalcResult<Quantity> {
        val source = unitOf(quantity)
        if (source.dimensions != target.dimensions) {
            return CalcError.UnitMismatch(source, target, span).err()
        }

        val base = source.offset.add(quantity.value.multiply(source.scale, MATH_CONTEXT), MATH_CONTEXT)
        val converted = base.subtract(target.offset, MATH_CONTEXT).divide(target.scale, MATH_CONTEXT)
        return Quantity(converted, target.orNull()).ok()
    }

    private fun combineAdditive(
        left: Quantity,
        right: Quantity,
        span: IntRange,
        subtract: Boolean,
    ): CalcResult<Quantity> {
        val leftUnit = unitOf(left)
        val rightUnit = unitOf(right)

        if (leftUnit.isAbsoluteTemperature || rightUnit.isAbsoluteTemperature) {
            return temperatureAdditive(left, right, span, subtract)
        }

        if (!leftUnit.isEmpty && !rightUnit.isEmpty && leftUnit.dimensions != rightUnit.dimensions) {
            return CalcError.UnitMismatch(leftUnit, rightUnit, span).err()
        }

        val rightInLeft = rescale(right.value, rightUnit, leftUnit)
        val value = if (subtract) {
            left.value.subtract(rightInLeft, MATH_CONTEXT)
        } else {
            left.value.add(rightInLeft, MATH_CONTEXT)
        }
        val unit = if (leftUnit.isEmpty) rightUnit else leftUnit
        return Quantity(value, unit.orNull()).ok()
    }

    private fun temperatureAdditive(
        left: Quantity,
        right: Quantity,
        span: IntRange,
        subtract: Boolean,
    ): CalcResult<Quantity> {
        val leftUnit = unitOf(left)
        val rightUnit = unitOf(right)
        val bothAbsolute = leftUnit.isAbsoluteTemperature && rightUnit.isAbsoluteTemperature

        return when {
            // 20°C + 5°C is meaningless: two absolute temperatures do not add.
            bothAbsolute && !subtract -> CalcError.UnitMismatch(leftUnit, rightUnit, span).err()
            bothAbsolute -> temperatureDifference(left, right, span)
            else -> temperatureWithDelta(left, right, span, subtract)
        }
    }

    /** `30°C - 20°C` is a difference, so the result is a delta in the left degree size. */
    private fun temperatureDifference(left: Quantity, right: Quantity, span: IntRange): CalcResult<Quantity> {
        val leftUnit = unitOf(left)
        val rightUnit = unitOf(right)
        val delta = deltaUnitOf(leftUnit) ?: return CalcError.UnitMismatch(leftUnit, rightUnit, span).err()
        val rightInLeft = right.value
            .multiply(rightUnit.scale, MATH_CONTEXT)
            .divide(leftUnit.scale, MATH_CONTEXT)
        return Quantity(left.value.subtract(rightInLeft, MATH_CONTEXT), delta).ok()
    }

    /** Absolute ± delta (or a bare number) stays absolute; the delta may need rescaling. */
    private fun temperatureWithDelta(
        left: Quantity,
        right: Quantity,
        span: IntRange,
        subtract: Boolean,
    ): CalcResult<Quantity> {
        val leftUnit = unitOf(left)
        val rightUnit = unitOf(right)
        val absoluteOnLeft = leftUnit.isAbsoluteTemperature
        val absolute = if (absoluteOnLeft) leftUnit else rightUnit
        val other = if (absoluteOnLeft) rightUnit else leftUnit

        if (!other.isEmpty && !other.isTemperatureDelta) {
            return CalcError.UnitMismatch(leftUnit, rightUnit, span).err()
        }

        val absoluteValue = if (absoluteOnLeft) left.value else right.value
        val deltaValue = if (absoluteOnLeft) rescale(right.value, other, absolute) else left.value
        val value = if (subtract) {
            absoluteValue.subtract(deltaValue, MATH_CONTEXT)
        } else {
            absoluteValue.add(deltaValue, MATH_CONTEXT)
        }
        return Quantity(value, absolute).ok()
    }

    private fun rejectAffine(left: Quantity, right: Quantity, span: IntRange): CalcResult<Quantity>? {
        val leftUnit = unitOf(left)
        val rightUnit = unitOf(right)
        return if (leftUnit.isAbsoluteTemperature || rightUnit.isAbsoluteTemperature) {
            CalcError.UnitMismatch(leftUnit, rightUnit, span).err()
        } else {
            null
        }
    }

    /** Expresses [value], given in [from], in terms of [to]. Both must share dimensions. */
    private fun rescale(value: BigDecimal, from: UnitTerm, to: UnitTerm): BigDecimal = when {
        from.isEmpty || to.isEmpty -> value
        from == to -> value
        else -> value.multiply(from.scale, MATH_CONTEXT).divide(to.scale, MATH_CONTEXT)
    }

    private fun deltaUnitOf(absolute: UnitTerm): UnitTerm? {
        val definition = absolute.factors.singleOrNull()?.definition ?: return null
        return UnitRegistry.deltaOf(definition)?.let { UnitTerm.of(it) }
    }

    private fun unitOf(quantity: Quantity): UnitTerm = quantity.unit ?: UnitTerm.NONE

    private fun UnitTerm.orNull(): UnitTerm? = takeUnless { it.isEmpty }
}
