package app.lineo.scientific

import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.DomainReason
import app.lineo.engine.EvalContext
import app.lineo.engine.Quantity
import app.lineo.engine.QuantityArithmetic
import app.lineo.engine.flatMap
import app.lineo.registry.CalcFunction
import app.lineo.registry.ParamKind
import app.lineo.registry.ParamSpec
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * What this module adds to the engine's own scientific set.
 *
 * `:core:engine` already owns `sin`, `ln`, `nCr` and the rest (P0-09), and
 * `ModuleRegistry` drops a module function whose name is taken — deliberately, so the same
 * expression cannot mean different things in different builds. Registering them again here
 * would therefore be dead code pretending to be a contribution. What is registered instead
 * is the set the engine does not have.
 *
 * **Every function here is composed from built-ins or from `BigDecimal` alone.** None of
 * them does `Double` arithmetic of its own: the engine re-boxes the transcendental path at
 * 15 significant digits and answers exact quarter turns exactly (`function/Precision.kt`),
 * and a second copy of that policy in a feature module would drift from it. It is also why
 * `atan2` is absent — it needs a `Double` this module must not take, so it belongs in
 * `:core:engine` if it is ever wanted.
 *
 * The reciprocal trigonometry is why the composition matters: `sec(90°)` is `1 / cos(90°)`,
 * and because the engine answers `cos(90°)` as exactly `0` rather than as `6.12e-17`, this
 * is an error the user is told about instead of an enormous number they are not.
 */
internal object ScientificFunctions {

    /**
     * The span a module error carries before the registry replaces it.
     *
     * A module cannot see the source text, so any span it invents is wrong; `CalcFunction`
     * re-spans whatever comes back onto the call site. This is the placeholder that gets
     * overwritten, and nothing may rely on its value.
     */
    private val NO_SPAN = 0..0

    private val ONE = Quantity(BigDecimal.ONE)

    /** How many arguments `min` and `max` accept. Beyond this a user wants a document, not a call. */
    private val EXTREMUM_ARITY = 1..8

    // Declared after what it reads. An object initialises in source order, so a list built
    // from properties below it would be built from nulls.
    val ALL: List<CalcFunction> = listOf(
        reciprocal("sec", of = "cos"),
        reciprocal("csc", of = "sin", aliases = listOf("cosec")),
        cotangent(),
        sign(),
        truncate(),
        extremum("min") { candidate -> candidate < 0 },
        extremum("max") { candidate -> candidate > 0 },
        hypotenuse(),
    )

    private fun reciprocal(name: String, of: String, aliases: List<String> = emptyList()) = CalcFunction(
        name = name,
        aliases = aliases,
        arity = 1..1,
        signature = listOf(ParamSpec("angle", ParamKind.ANGLE)),
    ) { arguments, context ->
        call(of, arguments, context).flatMap { divide(ONE, it, name) }
    }

    /**
     * `cot` as `cos / sin` rather than as `1 / tan`.
     *
     * They differ exactly where it matters: `tan(90°)` is a domain error, so `1 / tan(90°)`
     * would report one, while `cot(90°)` is `0` and is what a user reading a table expects.
     */
    private fun cotangent() = CalcFunction(
        name = "cot",
        aliases = listOf("cotan"),
        arity = 1..1,
        signature = listOf(ParamSpec("angle", ParamKind.ANGLE)),
    ) { arguments, context ->
        call("cos", arguments, context).flatMap { cosine ->
            call("sin", arguments, context).flatMap { sine -> divide(cosine, sine, "cot") }
        }
    }

    /** `-1`, `0` or `1`, and never a unit: the sign of `-5 km` is a number, not a length. */
    private fun sign() = CalcFunction(
        name = "sign",
        aliases = listOf("signum"),
        arity = 1..1,
        signature = listOf(ParamSpec("x", ParamKind.QUANTITY)),
    ) { arguments, _ ->
        CalcResult.Ok(Quantity(BigDecimal(arguments.first().value.signum())))
    }

    /**
     * Towards zero, which is the one rounding the engine does not have.
     *
     * `floor(-2.5)` is `-3` and `trunc(-2.5)` is `-2`; a user converting a measurement to
     * whole units means the second one and has no way to say it otherwise. The unit is kept,
     * as it is for `abs` and `floor`.
     */
    private fun truncate() = CalcFunction(
        name = "trunc",
        arity = 1..1,
        signature = listOf(ParamSpec("x", ParamKind.QUANTITY)),
    ) { arguments, _ ->
        val argument = arguments.first()
        CalcResult.Ok(argument.copy(value = argument.value.setScale(0, RoundingMode.DOWN)))
    }

    /**
     * `min` and `max`, over quantities rather than over numbers.
     *
     * Comparison goes through [QuantityArithmetic.subtract], so `min(1 km, 900 m)` converts
     * before it compares and `min(1 km, 5 kg)` is the same `UnitMismatch` the `-` operator
     * would report. Writing the comparison by hand would mean a second unit policy in a
     * feature module.
     *
     * The result is the argument itself, not a converted copy: `min(1 km, 900 m)` is
     * `900 m`, in the unit the user wrote it in.
     */
    private fun extremum(name: String, wins: (Int) -> Boolean) = CalcFunction(
        name = name,
        arity = EXTREMUM_ARITY,
        signature = listOf(ParamSpec("x", ParamKind.QUANTITY), ParamSpec("y", ParamKind.QUANTITY, optional = true)),
    ) { arguments, _ ->
        arguments.drop(1).fold(CalcResult.Ok(arguments.first()) as CalcResult<Quantity>) { best, candidate ->
            best.flatMap { incumbent ->
                QuantityArithmetic.subtract(candidate, incumbent, NO_SPAN).flatMap { difference ->
                    CalcResult.Ok(if (wins(difference.value.signum())) candidate else incumbent)
                }
            }
        }
    }

    /**
     * `sqrt(x² + y²)`, computed through the built-in `sqrt` so the root keeps one implementation.
     *
     * Plain numbers only. `sqrt` rejects a unit, so `hypot(3 km, 4 km)` is a domain error
     * rather than a silently dimensionless `5` — the engine's rule, reached rather than
     * restated.
     */
    private fun hypotenuse() = CalcFunction(
        name = "hypot",
        arity = 2..2,
        signature = listOf(ParamSpec("x"), ParamSpec("y")),
    ) { arguments, context ->
        squared(arguments[0]).flatMap { first ->
            squared(arguments[1]).flatMap { second ->
                QuantityArithmetic.add(first, second, NO_SPAN).flatMap { sum ->
                    call("sqrt", listOf(sum), context)
                }
            }
        }
    }

    private fun squared(quantity: Quantity): CalcResult<Quantity> =
        QuantityArithmetic.multiply(quantity, quantity, NO_SPAN)

    /**
     * Calls a function that is already registered.
     *
     * The registry comes from the context rather than from `FunctionRegistry.BUILTIN`, so a
     * build that replaced a built-in would have this follow it rather than quietly keep the
     * old one. A missing name is reported as unknown, which is what it is.
     */
    private fun call(name: String, arguments: List<Quantity>, context: EvalContext): CalcResult<Quantity> {
        val function = context.functions.find(name)
            ?: return CalcResult.Err(CalcError.UnknownIdentifier(name, suggestion = null, span = NO_SPAN))
        return function.evaluate(arguments, context, NO_SPAN)
    }

    /**
     * Divides, and reports a zero denominator as this function being undefined.
     *
     * `csc(0)` really is `1 / 0`, but "division by zero" points at arithmetic the user never
     * wrote. `DomainError(csc, UNDEFINED)` names the function they did write.
     */
    private fun divide(numerator: Quantity, denominator: Quantity, fn: String): CalcResult<Quantity> =
        when (val result = QuantityArithmetic.divide(numerator, denominator, NO_SPAN)) {
            is CalcResult.Ok -> {
                val quotient = trimmed(result.value.value, numerator, denominator)
                CalcResult.Ok(result.value.copy(value = quotient))
            }
            is CalcResult.Err -> when (result.error) {
                CalcError.DivisionByZero -> CalcResult.Err(CalcError.DomainError(fn, DomainReason.UNDEFINED, NO_SPAN))
                else -> result
            }
        }

    /**
     * A quotient may not claim more significant digits than the values it was divided from.
     *
     * `cos(45°)` and `sin(45°)` are the same number, but each arrives from a different
     * `Double` and they differ in the last digit the engine keeps — so `cos / sin` at
     * DECIMAL128 produced `1.000000000000001414213562373096098` for `cot(45)`, where every
     * digit after the first is an artefact of the division and not of the angle.
     *
     * The digits kept are read from the operands rather than restated as a number here. The
     * engine's own transcendental precision is `:core:engine`'s to choose (`Precision`, which
     * is internal to it, and rightly so); this asks the values themselves how precise they
     * are, so if the engine ever keeps one more digit, this follows without being edited.
     */
    private fun trimmed(quotient: BigDecimal, numerator: Quantity, denominator: Quantity): BigDecimal {
        val digits = maxOf(numerator.value.precision(), denominator.value.precision())
        return quotient.round(MathContext(digits, RoundingMode.HALF_UP)).stripTrailingZeros()
    }
}
