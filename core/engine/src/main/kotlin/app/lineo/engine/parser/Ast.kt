package app.lineo.engine.parser

import java.math.BigDecimal

/**
 * The parsed expression. Every node carries the [span] of the source it came from, which is
 * what lets the editor underline the exact characters that failed
 * (`docs/ARCHITECTURE.md` §3).
 */
sealed interface Ast {
    val span: IntRange

    data class NumberLiteral(val value: BigDecimal, override val span: IntRange) : Ast

    /**
     * A bare name. Resolution order is a variable, then a constant, then a function, then a
     * unit (`docs/GRAMMAR.md` §3.1) — and that happens in the evaluator, not here.
     */
    data class Identifier(val name: String, override val span: IntRange) : Ast

    /** `line3` or `@3` — a reference to a stable line id (`docs/GRAMMAR.md` §3.7). */
    data class LineReference(val line: Int, override val span: IntRange) : Ast

    data class Unary(val operator: UnaryOperator, val operand: Ast, override val span: IntRange) : Ast

    data class Binary(
        val operator: BinaryOperator,
        val left: Ast,
        val right: Ast,
        override val span: IntRange,
    ) : Ast

    data class Postfix(val operator: PostfixOperator, val operand: Ast, override val span: IntRange) : Ast

    data class Call(val name: String, val arguments: List<Ast>, override val span: IntRange) : Ast

    /** `5 km to mi` — [target] is a unit expression, validated by the parser. */
    data class Conversion(val value: Ast, val target: Ast, override val span: IntRange) : Ast

    /** `total = price - discount`. Only ever the outermost node of a line. */
    data class Assignment(val name: String, val value: Ast, override val span: IntRange) : Ast
}

enum class UnaryOperator { NEGATE, PLUS }

enum class BinaryOperator {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    MODULO,
    POWER,

    /** Adjacency, as in `2x` or `6/2(1+3)`. Kept distinct so percent can tell the two apart. */
    IMPLICIT_MULTIPLY,

    /** `50% of 80` (`docs/GRAMMAR.md` §3.6). */
    OF,
}

enum class PostfixOperator { PERCENT, FACTORIAL, DEGREE }
