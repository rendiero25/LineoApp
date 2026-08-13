package app.lineo.engine

import java.math.BigDecimal

/**
 * Every value in the engine is a quantity, not a bare number (`docs/ARCHITECTURE.md` §2).
 *
 * A `null` [unit] means dimensionless. Arithmetic lives in P0-05; this type exists from
 * the start so the evaluator never has to be rewritten around it.
 */
data class Quantity(
    val value: BigDecimal,
    val unit: UnitTerm? = null,
) {
    /**
     * Locale-free rendering, used internally and by the golden tests. Display formatting
     * is a UI-boundary concern (`docs/CONVENTIONS.md` §1) and never happens here.
     */
    fun canonicalString(): String {
        val number = value.stripTrailingZeros().toPlainString()
        return if (unit == null) number else "$number ${unit.symbol}"
    }
}

/**
 * A unit expression. Full base-dimension exponents and scale factors arrive in P0-05;
 * for now it carries the symbol so error reporting and formatting already work.
 */
data class UnitTerm(val symbol: String)
