package app.lineo.engine.function

import app.lineo.engine.CalcResult
import app.lineo.engine.EvalContext
import app.lineo.engine.Quantity
import app.lineo.engine.flatMap
import app.lineo.engine.ok
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * The scientific function set every surface can rely on (P0-09).
 *
 * These are registered the same way a module's functions are (`docs/ARCHITECTURE.md` §4):
 * there is no privileged path for built-ins, so `:feature:scientific` adding a function and
 * this list are the same mechanism. Anything here is callable as text in notepad mode.
 *
 * Unit policy, per family:
 *
 * - Trigonometry takes an angle — a bare number read in the angle mode, or a quantity in
 *   `°`, `rad` or `grad`.
 * - Logarithms, roots and combinatorics take plain numbers; a unit is a domain error.
 * - `abs`, `floor`, `ceil`, `round` and `mod` keep the unit they were given: `abs(-5 km)`
 *   is `5 km`.
 */
object BuiltinFunctions {

    val ALL: List<EngineFunction> = buildList {
        addAll(circular())
        addAll(hyperbolic())
        addAll(exponential())
        addAll(rounding())
        addAll(integers())
        addAll(counting())
    }

    private fun circular(): List<EngineFunction> = listOf(
        unary("sin") { argument, context, span -> Trigonometry.sin(argument, context.angleMode, span) },
        unary("cos") { argument, context, span -> Trigonometry.cos(argument, context.angleMode, span) },
        unary("tan") { argument, context, span -> Trigonometry.tan(argument, context.angleMode, span) },
        unary("asin", "arcsin") { argument, context, span -> Trigonometry.asin(argument, context.angleMode, span) },
        unary("acos", "arccos") { argument, context, span -> Trigonometry.acos(argument, context.angleMode, span) },
        unary("atan", "arctan") { argument, context, span -> Trigonometry.atan(argument, context.angleMode, span) },
    )

    private fun hyperbolic(): List<EngineFunction> = listOf(
        unary("sinh") { argument, _, span -> Hyperbolics.sinh(argument, span) },
        unary("cosh") { argument, _, span -> Hyperbolics.cosh(argument, span) },
        unary("tanh") { argument, _, span -> Hyperbolics.tanh(argument, span) },
        unary("asinh", "arsinh") { argument, _, span -> Hyperbolics.asinh(argument, span) },
        unary("acosh", "arcosh") { argument, _, span -> Hyperbolics.acosh(argument, span) },
        unary("atanh", "artanh") { argument, _, span -> Hyperbolics.atanh(argument, span) },
    )

    private fun exponential(): List<EngineFunction> = listOf(
        unary("ln") { argument, _, span -> Exponentials.naturalLog(argument, span) },
        unary("log2") { argument, _, span -> Exponentials.log2(argument, span) },
        unary("exp") { argument, _, span -> Exponentials.exponential(argument, span) },
        unary("sqrt", "√") { argument, _, span -> Exponentials.squareRoot(argument, span) },
        unary("cbrt") { argument, _, span -> Exponentials.cubeRoot(argument, span) },
        binary("root") { argument, degree, span -> Exponentials.nthRoot(argument, degree, span) },
        // log(x) is base 10; log(x, b) is base b. Both spellings are common on calculators.
        EngineFunction("log", 1..2) { arguments, _, span ->
            if (arguments.size == 1) {
                Exponentials.log10(arguments[0], span)
            } else {
                Exponentials.logBase(arguments[0], arguments[1], span)
            }
        },
    )

    private fun rounding(): List<EngineFunction> = listOf(
        unary("abs") { argument, _, _ -> Quantity(argument.value.abs(), argument.unit).ok() },
        unary("floor") { argument, _, _ -> scaled(argument, 0, RoundingMode.FLOOR).ok() },
        unary("ceil") { argument, _, _ -> scaled(argument, 0, RoundingMode.CEILING).ok() },
        // round(x) to a whole number, round(x, d) to d decimal places. HALF_UP, never
        // banker's rounding — docs/CONVENTIONS.md §4.
        EngineFunction("round", 1..2) { arguments, _, span ->
            if (arguments.size == 1) {
                scaled(arguments[0], 0, RoundingMode.HALF_UP).ok()
            } else {
                Arguments.count(arguments[1], ROUND, span, MAX_DECIMALS).flatMap { places ->
                    scaled(arguments[0], places, RoundingMode.HALF_UP).ok()
                }
            }
        },
    )

    // `mod` is deliberately absent: it is a reserved word (`docs/GRAMMAR.md` §4) and stays
    // the infix operator the evaluator already implements, so `10 mod 3` is the one spelling.
    private fun integers(): List<EngineFunction> = listOf(
        binary("gcd") { left, right, span -> wholePair(GCD, left, right, span) { a, b -> a.gcd(b) } },
        binary("lcm") { left, right, span -> wholePair(LCM, left, right, span, ::leastCommonMultiple) },
    )

    private fun counting(): List<EngineFunction> = listOf(
        unary("fact", "factorial") { argument, _, span -> Combinatorics.factorial(argument, FACT, span) },
        binary("nPr") { total, chosen, span -> Combinatorics.permutations(total, chosen, span) },
        binary("nCr") { total, chosen, span -> Combinatorics.combinations(total, chosen, span) },
    )

    private fun wholePair(
        fn: String,
        left: Quantity,
        right: Quantity,
        span: IntRange,
        compute: (BigInteger, BigInteger) -> BigInteger,
    ): CalcResult<Quantity> = Arguments.integer(left, fn, span).flatMap { a ->
        Arguments.integer(right, fn, span).flatMap { b ->
            Quantity(BigDecimal(compute(a, b))).ok()
        }
    }

    /** `lcm(a, b) = |a·b| / gcd(a, b)`, and zero with anything is zero. */
    private fun leastCommonMultiple(a: BigInteger, b: BigInteger): BigInteger =
        if (a.signum() == 0 || b.signum() == 0) BigInteger.ZERO else a.multiply(b).abs().divide(a.gcd(b))

    /**
     * Rounding never *adds* digits: a value already coarser than [places] is returned as it
     * stands. Without that, `floor(1e999999999)` would try to write out a billion digits.
     */
    private fun scaled(quantity: Quantity, places: Int, mode: RoundingMode): Quantity =
        if (quantity.value.scale() <= places) {
            quantity
        } else {
            Quantity(quantity.value.setScale(places, mode), quantity.unit)
        }

    private fun unary(
        name: String,
        vararg aliases: String,
        body: (Quantity, EvalContext, IntRange) -> CalcResult<Quantity>,
    ): EngineFunction = EngineFunction(name, 1..1, aliases.toList()) { arguments, context, span ->
        body(arguments[0], context, span)
    }

    private fun binary(
        name: String,
        vararg aliases: String,
        body: (Quantity, Quantity, IntRange) -> CalcResult<Quantity>,
    ): EngineFunction = EngineFunction(name, 2..2, aliases.toList()) { arguments, _, span ->
        body(arguments[0], arguments[1], span)
    }

    private const val ROUND = "round"
    private const val FACT = "fact"
    private const val GCD = "gcd"
    private const val LCM = "lcm"

    /** More decimal places than `MathContext.DECIMAL128` can distinguish are meaningless. */
    private const val MAX_DECIMALS = 34
}
