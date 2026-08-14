package app.lineo.engine.eval

import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.DomainReason
import app.lineo.engine.EvalContext
import app.lineo.engine.LineId
import app.lineo.engine.MATH_CONTEXT
import app.lineo.engine.Quantity
import app.lineo.engine.QuantityArithmetic
import app.lineo.engine.err
import app.lineo.engine.flatMap
import app.lineo.engine.function.Combinatorics
import app.lineo.engine.ok
import app.lineo.engine.parser.Ast
import app.lineo.engine.parser.BinaryOperator
import app.lineo.engine.parser.PostfixOperator
import app.lineo.engine.parser.UnaryOperator
import app.lineo.engine.unit.UnitRegistry
import app.lineo.engine.unit.UnitTerm
import java.math.BigDecimal

/**
 * Walks the AST and produces a value or a [CalcError] (`docs/ARCHITECTURE.md` §3).
 *
 * Nothing here throws: arithmetic overflow, bad domains and unknown names are all values.
 */
class Evaluator(private val context: EvalContext) {

    /** Names visible to a bare identifier, used for the nearest-match suggestion. */
    private val knownNames: Set<String>
        get() = context.variables.keys + Constants.names + context.functions.names + UnitRegistry.symbols

    fun evaluate(node: Ast): CalcResult<Quantity> = when (node) {
        is Ast.NumberLiteral -> Quantity(node.value).ok()
        is Ast.Identifier -> resolve(node.name, node.span)
        is Ast.LineReference -> resolveLine(node)
        is Ast.Assignment -> evaluate(node.value)
        is Ast.Unary -> evaluateUnary(node)
        is Ast.Postfix -> evaluatePostfix(node)
        is Ast.Binary -> evaluateBinary(node)
        is Ast.Conversion -> evaluateConversion(node)
        is Ast.Call -> evaluateCall(node)
    }

    /**
     * Resolution order of `docs/GRAMMAR.md` §3.1: a document variable wins over a constant,
     * a constant over a function name, and a function name over a unit. So after `m = 5`,
     * `2m` is `10` rather than two metres.
     */
    private fun resolve(name: String, span: IntRange): CalcResult<Quantity> {
        val variable = context.variables[name]
        val constant = Constants.find(name)
        val unit = UnitRegistry.find(name)

        return when {
            variable != null -> variable.ok()
            constant != null -> constant.ok()
            // A function name in value position is a call missing its arguments, not a value.
            context.functions.find(name) != null -> CalcError.Syntax(name, span).err()
            unit != null -> Quantity(BigDecimal.ONE, UnitTerm.of(unit)).ok()
            else -> CalcError.UnknownIdentifier(
                name = name,
                suggestion = Suggestions.nearest(name, knownNames),
                span = span,
            ).err()
        }
    }

    private fun resolveLine(node: Ast.LineReference): CalcResult<Quantity> =
        context.lineResults[LineId(node.line.toLong())]?.ok()
            ?: CalcError.UnknownIdentifier(
                name = "line${node.line}",
                suggestion = null,
                span = node.span,
            ).err()

    private fun evaluateUnary(node: Ast.Unary): CalcResult<Quantity> = evaluate(node.operand).flatMap { operand ->
        when (node.operator) {
            UnaryOperator.NEGATE -> QuantityArithmetic.negate(operand).ok()
            UnaryOperator.PLUS -> operand.ok()
        }
    }

    private fun evaluatePostfix(node: Ast.Postfix): CalcResult<Quantity> = evaluate(node.operand).flatMap { operand ->
        when (node.operator) {
            // Standalone percent is a hundredth: 10% is 0.1 (docs/GRAMMAR.md §3.6).
            PostfixOperator.PERCENT -> Quantity(
                operand.value.divide(HUNDRED, MATH_CONTEXT),
                operand.unit,
            ).ok()

            PostfixOperator.FACTORIAL -> factorial(operand, node.span)
            PostfixOperator.DEGREE -> degrees(operand, node.span)
        }
    }

    /**
     * Percent is contextual (`docs/GRAMMAR.md` §3.6): in additive position `100 + 10%` means
     * `100 + 100 × 0.10`, while in multiplicative position `100 * 10%` means `100 × 0.10`.
     */
    private fun evaluateBinary(node: Ast.Binary): CalcResult<Quantity> {
        if (node.operator == BinaryOperator.OF) return evaluateOf(node)

        // `20°C` and `5 km` are adjacency, but they attach a unit rather than multiply by
        // one. The difference matters for affine units, which never take part in a product.
        if (node.operator == BinaryOperator.IMPLICIT_MULTIPLY) {
            unitOf(node.right)?.let { unit ->
                return evaluate(node.left).flatMap { value -> attachUnit(value, unit, node.span) }
            }
        }

        return evaluate(node.left).flatMap { left ->
            val additive = node.operator == BinaryOperator.ADD || node.operator == BinaryOperator.SUBTRACT
            val percentRight = node.right as? Ast.Postfix

            if (additive && percentRight?.operator == PostfixOperator.PERCENT) {
                evaluate(percentRight.operand).flatMap { percent ->
                    val share = left.value
                        .multiply(percent.value, MATH_CONTEXT)
                        .divide(HUNDRED, MATH_CONTEXT)
                    apply(node.operator, left, Quantity(share, left.unit), node.span)
                }
            } else {
                evaluate(node.right).flatMap { right -> apply(node.operator, left, right, node.span) }
            }
        }
    }

    /** The unit a bare name denotes, or null when it is a variable, a constant or unknown. */
    private fun unitOf(node: Ast): UnitTerm? {
        val identifier = node as? Ast.Identifier ?: return null
        if (identifier.name in context.variables || identifier.name in Constants.names) return null
        return UnitRegistry.find(identifier.name)?.let { UnitTerm.of(it) }
    }

    private fun attachUnit(value: Quantity, unit: UnitTerm, span: IntRange): CalcResult<Quantity> =
        if (value.isDimensionless) {
            Quantity(value.value, unit).ok()
        } else {
            QuantityArithmetic.multiply(value, Quantity(BigDecimal.ONE, unit), span)
        }

    /** `50% of 80` is `80 × 50/100` (`docs/GRAMMAR.md` §3.6). */
    private fun evaluateOf(node: Ast.Binary): CalcResult<Quantity> =
        evaluate(node.left).flatMap { share ->
            evaluate(node.right).flatMap { whole ->
                QuantityArithmetic.multiply(whole, share, node.span)
            }
        }

    private fun apply(
        operator: BinaryOperator,
        left: Quantity,
        right: Quantity,
        span: IntRange,
    ): CalcResult<Quantity> = when (operator) {
        BinaryOperator.ADD -> QuantityArithmetic.add(left, right, span)
        BinaryOperator.SUBTRACT -> QuantityArithmetic.subtract(left, right, span)
        BinaryOperator.MULTIPLY, BinaryOperator.IMPLICIT_MULTIPLY -> QuantityArithmetic.multiply(left, right, span)
        BinaryOperator.DIVIDE -> QuantityArithmetic.divide(left, right, span)
        BinaryOperator.MODULO -> modulo(left, right)
        BinaryOperator.POWER -> power(left, right, span)
        BinaryOperator.OF -> QuantityArithmetic.multiply(right, left, span)
    }

    private fun modulo(left: Quantity, right: Quantity): CalcResult<Quantity> =
        if (right.value.signum() == 0) {
            CalcError.DivisionByZero.err()
        } else {
            Quantity(left.value.remainder(right.value, MATH_CONTEXT), left.unit).ok()
        }

    private fun power(base: Quantity, exponent: Quantity, span: IntRange): CalcResult<Quantity> {
        if (!exponent.isDimensionless) {
            return CalcError.DomainError(POWER_NAME, DomainReason.OUT_OF_RANGE, span).err()
        }

        val whole = exponent.value.toWholeIntOrNull()
        return when {
            whole != null && kotlin.math.abs(whole) > MAX_POWER -> CalcError.Overflow(span).err()
            whole != null -> QuantityArithmetic.power(base, whole, span)
            !base.isDimensionless -> CalcError.DomainError(POWER_NAME, DomainReason.NON_INTEGER, span).err()
            else -> fractionalPower(base, exponent, span)
        }
    }

    /**
     * Fractional exponents go through `Double` and come back as `BigDecimal`, which
     * `docs/ARCHITECTURE.md` §2 permits for transcendental work only.
     */
    private fun fractionalPower(base: Quantity, exponent: Quantity, span: IntRange): CalcResult<Quantity> {
        val result = Math.pow(base.value.toDouble(), exponent.value.toDouble())
        return when {
            result.isNaN() -> CalcError.DomainError(POWER_NAME, DomainReason.UNDEFINED, span).err()
            result.isInfinite() -> CalcError.Overflow(span).err()
            else -> Quantity(BigDecimal(result, MATH_CONTEXT)).ok()
        }
    }

    /** `5!` and `fact(5)` are the same computation, so both come from [Combinatorics]. */
    private fun factorial(operand: Quantity, span: IntRange): CalcResult<Quantity> =
        Combinatorics.factorial(operand, FACTORIAL_NAME, span)

    /** `90°` is an angle in degrees, kept as a unit so trigonometry can honour it. */
    private fun degrees(operand: Quantity, span: IntRange): CalcResult<Quantity> {
        if (!operand.isDimensionless) {
            return CalcError.DomainError(DEGREE_NAME, DomainReason.OUT_OF_RANGE, span).err()
        }
        val degree = UnitRegistry.find(DEGREE_SYMBOL)
            ?: return CalcError.DomainError(DEGREE_NAME, DomainReason.UNDEFINED, span).err()
        return Quantity(operand.value, UnitTerm.of(degree)).ok()
    }

    private fun evaluateConversion(node: Ast.Conversion): CalcResult<Quantity> =
        evaluate(node.value).flatMap { value ->
            evaluate(node.target).flatMap { target ->
                val unit = target.unit
                if (unit == null) {
                    CalcError.Syntax(TARGET_NOT_A_UNIT, node.target.span).err()
                } else {
                    QuantityArithmetic.convert(value, unit, node.span)
                }
            }
        }

    private fun evaluateCall(node: Ast.Call): CalcResult<Quantity> {
        val function = context.functions.find(node.name)
            ?: return CalcError.UnknownIdentifier(
                name = node.name,
                suggestion = Suggestions.nearest(node.name, knownNames),
                span = spanOfName(node),
            ).err()

        val arguments = mutableListOf<Quantity>()
        node.arguments.forEach { argument ->
            when (val result = evaluate(argument)) {
                is CalcResult.Ok -> arguments += result.value
                is CalcResult.Err -> return result
            }
        }

        function.arityError(arguments, node.span)?.let { return it.err() }
        return function.evaluate(arguments, context, node.span)
    }

    /** A call's error points at the name, not at the whole call with its arguments. */
    private fun spanOfName(node: Ast.Call): IntRange =
        node.span.first until (node.span.first + node.name.length)

    private fun BigDecimal.toWholeIntOrNull(): Int? = try {
        stripTrailingZeros().toBigIntegerExact().toInt().takeIf {
            BigDecimal(it).compareTo(stripTrailingZeros()) == 0
        }
    } catch (_: ArithmeticException) {
        null
    }

    private companion object {
        val HUNDRED = BigDecimal("100")
        const val MAX_POWER = 100_000
        const val POWER_NAME = "^"
        const val FACTORIAL_NAME = "!"
        const val DEGREE_NAME = "°"
        const val DEGREE_SYMBOL = "°"
        const val TARGET_NOT_A_UNIT = "unit"
    }
}
