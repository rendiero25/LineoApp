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
 * Factorial, permutations and combinations.
 *
 * Exact throughout — these are counts, so they run on `BigInteger` and never touch
 * [Precision]. The postfix `!` operator and the `fact` function both come here, which is
 * what keeps `5!` and `fact(5)` from ever disagreeing.
 */
internal object Combinatorics {

    /**
     * `1000!` already has 2568 digits. Past this the result is of no use on a phone screen
     * and the multiplication starts to cost real time, so it is a domain error, not a wait.
     */
    const val MAX_INPUT = 1_000

    /** [fn] is the caller's name: `!` for the operator, `fact` for the function. */
    fun factorial(argument: Quantity, fn: String, span: IntRange): CalcResult<Quantity> =
        Arguments.count(argument, fn, span, MAX_INPUT).flatMap { n ->
            if (n < 0) {
                CalcError.DomainError(fn, DomainReason.NEGATIVE_INPUT, span).err()
            } else {
                Quantity(BigDecimal(factorialOf(n))).ok()
            }
        }

    /** `nPr(n, r)` — ordered selections: `n!/(n-r)!`, computed as the falling product. */
    fun permutations(total: Quantity, chosen: Quantity, span: IntRange): CalcResult<Quantity> =
        pair(NPR, total, chosen, span) { n, r -> fallingProduct(n, r) }

    /** `nCr(n, r)` — unordered selections: `nPr(n, r)/r!`, exact because the division is. */
    fun combinations(total: Quantity, chosen: Quantity, span: IntRange): CalcResult<Quantity> =
        pair(NCR, total, chosen, span) { n, r -> fallingProduct(n, r) / factorialOf(r) }

    private fun pair(
        fn: String,
        total: Quantity,
        chosen: Quantity,
        span: IntRange,
        compute: (Int, Int) -> BigInteger,
    ): CalcResult<Quantity> = Arguments.count(total, fn, span, MAX_INPUT).flatMap { n ->
        Arguments.count(chosen, fn, span, MAX_INPUT).flatMap { r ->
            when {
                n < 0 || r < 0 -> CalcError.DomainError(fn, DomainReason.NEGATIVE_INPUT, span).err()
                // Choosing 7 of 5 is not zero, it is a question that does not make sense.
                r > n -> CalcError.DomainError(fn, DomainReason.OUT_OF_RANGE, span).err()
                else -> Quantity(BigDecimal(compute(n, r))).ok()
            }
        }
    }

    private fun factorialOf(n: Int): BigInteger {
        var result = BigInteger.ONE
        for (factor in 2..n) result = result.multiply(BigInteger.valueOf(factor.toLong()))
        return result
    }

    /** `n × (n-1) × … × (n-r+1)`, the numerator both selections share. */
    private fun fallingProduct(n: Int, r: Int): BigInteger {
        var result = BigInteger.ONE
        for (factor in (n - r + 1)..n) result = result.multiply(BigInteger.valueOf(factor.toLong()))
        return result
    }

    private const val NPR = "nPr"
    private const val NCR = "nCr"
}
