package app.lineo.engine

import app.lineo.engine.unit.UnitTerm
import java.math.BigDecimal

/**
 * Every value in the engine is a quantity, not a bare number (`docs/ARCHITECTURE.md` §2).
 *
 * A `null` [unit] means dimensionless. All arithmetic runs at [MATH_CONTEXT]; `Double`
 * never appears in a result path.
 */
data class Quantity(
    val value: BigDecimal,
    val unit: UnitTerm? = null,
) {
    val isDimensionless: Boolean get() = unit == null || unit.isEmpty

    /**
     * Locale-free rendering, used internally and by the golden tests. Display formatting
     * is a UI-boundary concern (`docs/CONVENTIONS.md` §1) and never happens here.
     */
    fun canonicalString(): String {
        val number = value.stripTrailingZeros().toPlainString()
        return if (isDimensionless) number else "$number ${requireNotNull(unit).symbol}"
    }

    companion object {
        fun of(value: String, unit: UnitTerm? = null): Quantity = Quantity(BigDecimal(value), unit)
    }
}
