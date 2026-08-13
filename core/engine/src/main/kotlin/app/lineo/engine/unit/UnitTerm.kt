package app.lineo.engine.unit

import java.math.BigDecimal
import java.math.MathContext

/** One symbol raised to an exponent, e.g. `h^-1` in `km/h`. */
data class UnitFactor(val definition: UnitDefinition, val exponent: Int)

/**
 * A unit expression: a product of symbols with integer exponents.
 *
 * Keeping the symbols rather than collapsing to base units is what makes `2 h * 60 km/h`
 * come out as `120 km` — the `h` cancels symbolically (`docs/GRAMMAR.md` §3.8) — and what
 * lets `5 km + 300 m` keep the left operand's unit.
 */
data class UnitTerm(val factors: List<UnitFactor>) {

    init {
        require(factors.none { it.exponent == 0 }) { "a zero exponent must be cancelled out, not stored" }
    }

    val isEmpty: Boolean get() = factors.isEmpty()

    val dimensions: Dimensions
        get() = factors.fold(Dimensions.NONE) { acc, factor -> acc + factor.definition.dimensions * factor.exponent }

    /** Factor to the base unit of each dimension. Undefined for affine units, which never compose. */
    val scale: BigDecimal
        get() = factors.fold(BigDecimal.ONE) { acc, factor ->
            var value = acc
            repeat(kotlin.math.abs(factor.exponent)) {
                value = if (factor.exponent > 0) {
                    value.multiply(factor.definition.scale, MathContext.DECIMAL128)
                } else {
                    value.divide(factor.definition.scale, MathContext.DECIMAL128)
                }
            }
            value
        }

    /** The offset of an affine unit, or zero. Only a lone absolute temperature has one. */
    val offset: BigDecimal
        get() = singleDefinition()?.offset ?: BigDecimal.ZERO

    val kind: UnitKind
        get() = singleDefinition()?.kind ?: UnitKind.LINEAR

    val isAbsoluteTemperature: Boolean get() = kind == UnitKind.ABSOLUTE_TEMPERATURE

    val isTemperatureDelta: Boolean get() = kind == UnitKind.TEMPERATURE_DELTA

    /** ISO 80000 style rendering: `km`, `km/h`, `kg·m/s^2`. */
    val symbol: String
        get() {
            val positive = factors.filter { it.exponent > 0 }
            val negative = factors.filter { it.exponent < 0 }
            val numerator = if (positive.isEmpty()) "1" else positive.joinToString("·") { render(it.exponent, it) }
            val denominator = negative.joinToString("·") { render(-it.exponent, it) }
            return if (denominator.isEmpty()) numerator else "$numerator/$denominator"
        }

    operator fun times(other: UnitTerm): UnitTerm = combine(other, sign = 1)

    operator fun div(other: UnitTerm): UnitTerm = combine(other, sign = -1)

    fun pow(exponent: Int): UnitTerm =
        UnitTerm(factors.map { UnitFactor(it.definition, it.exponent * exponent) }.filter { it.exponent != 0 })

    private fun combine(other: UnitTerm, sign: Int): UnitTerm {
        val merged = LinkedHashMap<String, UnitFactor>()
        factors.forEach { merged[it.definition.symbol] = it }
        other.factors.forEach { factor ->
            val existing = merged[factor.definition.symbol]
            val exponent = (existing?.exponent ?: 0) + factor.exponent * sign
            if (exponent == 0) {
                merged.remove(factor.definition.symbol)
            } else {
                merged[factor.definition.symbol] = UnitFactor(factor.definition, exponent)
            }
        }
        return UnitTerm(merged.values.toList())
    }

    private fun singleDefinition(): UnitDefinition? =
        factors.singleOrNull()?.takeIf { it.exponent == 1 }?.definition

    private fun render(exponent: Int, factor: UnitFactor): String =
        if (exponent == 1) factor.definition.symbol else "${factor.definition.symbol}^$exponent"

    companion object {
        val NONE = UnitTerm(emptyList())

        fun of(definition: UnitDefinition, exponent: Int = 1): UnitTerm =
            if (exponent == 0) NONE else UnitTerm(listOf(UnitFactor(definition, exponent)))
    }
}
