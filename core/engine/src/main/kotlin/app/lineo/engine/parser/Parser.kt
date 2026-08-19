package app.lineo.engine.parser

import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.err
import app.lineo.engine.lexer.Token
import app.lineo.engine.lexer.TokenType
import app.lineo.engine.ok

/**
 * Hand-written Pratt parser. Binding powers come straight from the precedence table in
 * `docs/GRAMMAR.md` §1:
 *
 * | Level | Operators | Binding power |
 * |---|---|---|
 * | 1 | `to` `in` `as` | [CONVERSION] |
 * | 2 | `+` `-` | [ADDITIVE] |
 * | 3 | `*` `/` `mod` `of` | [MULTIPLICATIVE] |
 * | 4 | implicit multiplication | [MULTIPLICATIVE], see below |
 * | 5 | unary `-` `+` | [UNARY] |
 * | 6 | `^`, right-associative | [POWER] |
 * | 7 | postfix `%` `!` `°` | [POSTFIX] |
 *
 * Implicit multiplication is listed one level above `*` in the table but evaluates at the
 * same effective level, which is what makes `6/2(1+3)` equal `12`. Unary minus sits below
 * `^`, which is what makes `-2^2` equal `-4`.
 *
 * The parser never throws: [parse] returns a [CalcResult] and every failure is a
 * [CalcError] with a span.
 *
 * [knownFunctions] lets `sin 30` parse without parentheses (`docs/GRAMMAR.md` §3.5); a name
 * the engine does not know is never treated as a bare call.
 */
class Parser(
    private val tokens: List<Token>,
    private val knownFunctions: Set<String> = emptySet(),
) {
    private var position = 0
    private var depth = 0

    fun parse(): CalcResult<Ast> = try {
        val ast = parseLine()
        expectEnd()
        ast.ok()
    } catch (failure: ParseFailure) {
        failure.error.err()
    }

    private fun parseLine(): Ast {
        val name = peek()
        if (name.type == TokenType.IDENTIFIER && peek(1).type == TokenType.ASSIGN) {
            advance()
            advance()
            val value = parseExpression(0)
            return Ast.Assignment(name.text, value, name.span.first..value.span.last)
        }
        return parseExpression(0)
    }

    private fun parseExpression(minBindingPower: Int): Ast {
        // Nesting is bounded so that pathological input fails as an error rather than as a
        // stack overflow — the fuzzing contract of `docs/GRAMMAR.md` §6.
        if (++depth > MAX_DEPTH) fail(peek())
        try {
            var left = parsePrefix()
            while (true) {
                left = extend(left, minBindingPower) ?: return left
            }
        } finally {
            depth--
        }
    }

    /** Consumes one infix operator, unit-position `in`, or adjacency. Null when nothing binds. */
    private fun extend(left: Ast, minBindingPower: Int): Ast? {
        val token = peek()

        val conversionKeyword = token.type == TokenType.KEYWORD && token.text in CONVERSION_KEYWORDS
        if (conversionKeyword && isUnitPositionIn(token)) {
            return implicitMultiply(left, Ast.Identifier(INCH, advance().span))
        }

        val bindingPower = infixBindingPower(token)
        if (bindingPower != null && bindingPower > minBindingPower) {
            return parseInfix(left, advance(), bindingPower)
        }

        if (MULTIPLICATIVE > minBindingPower && startsPrimary(token)) {
            return implicitMultiply(left, parseUnaryOperand(MULTIPLICATIVE))
        }

        return null
    }

    private fun parseInfix(left: Ast, operator: Token, bindingPower: Int): Ast = when (operator.type) {
        TokenType.CARET -> {
            // Right-associative: 2^3^2 is 2^(3^2).
            val right = parseExpression(bindingPower - 1)
            Ast.Binary(BinaryOperator.POWER, left, right, left.span.first..right.span.last)
        }

        TokenType.KEYWORD if operator.text in CONVERSION_KEYWORDS -> {
            val target = parseConversionTarget(bindingPower)
            requireUnitExpression(target)
            Ast.Conversion(left, target, left.span.first..target.span.last)
        }

        else -> {
            val right = parseExpression(bindingPower)
            Ast.Binary(binaryOperator(operator), left, right, left.span.first..right.span.last)
        }
    }

    /**
     * The right-hand side of `to`, `in` or `as`, where `in` can only be the inch.
     *
     * `5 cm to in` was a syntax error: §3.3 makes a bare `in` the conversion keyword and only
     * the inch directly after a number, and a target starting with a keyword parses as
     * nothing. But a conversion target cannot *be* a conversion — `5 cm to in mm` has no
     * reading in which the first `in` converts — so in this one position the ambiguity the
     * rule exists to resolve does not arise, and the unit wins.
     *
     * Everything after it parses as usual, so `to in^2` and `to in/s` work too. `5 in 3` is
     * untouched and still reports `Syntax` on the `3`.
     */
    private fun parseConversionTarget(bindingPower: Int): Ast {
        val token = peek()
        if (token.type != TokenType.KEYWORD || token.text != INCH) return parseExpression(bindingPower)
        var target: Ast = Ast.Identifier(INCH, advance().span)
        while (true) {
            target = extend(target, bindingPower) ?: return target
        }
    }

    private fun parsePrefix(): Ast {
        val token = peek()
        return when (token.type) {
            TokenType.MINUS, TokenType.PLUS -> {
                advance()
                val operand = parseExpression(UNARY)
                val operator = if (token.type == TokenType.MINUS) UnaryOperator.NEGATE else UnaryOperator.PLUS
                Ast.Unary(operator, operand, token.span.first..operand.span.last)
            }

            else -> parsePostfix(parsePrimary())
        }
    }

    /**
     * Operand of an implicit multiplication or of a parenthesis-free call. It binds tighter
     * than the caller's level, which is how `sin 2x` becomes `sin(2x)` while `sin 30 + 1`
     * stays `sin(30) + 1`.
     */
    private fun parseUnaryOperand(minBindingPower: Int): Ast {
        var operand = parsePrefix()
        while (true) {
            val token = peek()
            val bindingPower = infixBindingPower(token)
            when {
                bindingPower != null && bindingPower > minBindingPower ->
                    operand = parseInfix(operand, advance(), bindingPower)

                MULTIPLICATIVE > minBindingPower && startsPrimary(token) ->
                    operand = implicitMultiply(operand, parsePrefix())

                else -> return operand
            }
        }
    }

    private fun parsePostfix(operand: Ast): Ast {
        var result = operand
        while (true) {
            val token = peek()
            val operator = when (token.type) {
                TokenType.PERCENT -> PostfixOperator.PERCENT
                TokenType.BANG -> PostfixOperator.FACTORIAL
                TokenType.DEGREE -> PostfixOperator.DEGREE
                else -> return result
            }
            advance()
            result = Ast.Postfix(operator, result, result.span.first..token.span.last)
        }
    }

    private fun parsePrimary(): Ast {
        val token = advance()
        return when (token.type) {
            TokenType.NUMBER -> Ast.NumberLiteral(requireNotNull(token.number), token.span)

            TokenType.LINE_REF -> Ast.LineReference(requireNotNull(token.lineNumber), token.span)

            TokenType.IDENTIFIER -> parseIdentifier(token)

            TokenType.LEFT_PAREN -> parseGroup(token)

            else -> fail(token)
        }
    }

    private fun parseIdentifier(token: Token): Ast = when {
        peek().type == TokenType.LEFT_PAREN -> parseCall(token)

        // sin 30 — a single-argument built-in may drop its parentheses (docs/GRAMMAR.md §3.5).
        token.text in knownFunctions && startsPrimary(peek()) -> {
            val argument = parseUnaryOperand(MULTIPLICATIVE - 1)
            Ast.Call(token.text, listOf(argument), token.span.first..argument.span.last)
        }

        else -> Ast.Identifier(token.text, token.span)
    }

    private fun parseCall(name: Token): Ast {
        val open = advance()
        val arguments = mutableListOf<Ast>()

        if (peek().type != TokenType.RIGHT_PAREN) {
            while (true) {
                arguments += parseExpression(0)
                if (peek().type != TokenType.ARG_SEPARATOR) break
                advance()
            }
        }

        val close = expectRightParen(open)
        return Ast.Call(name.text, arguments, name.span.first..close.span.last)
    }

    private fun parseGroup(open: Token): Ast {
        val inner = parseExpression(0)
        val close = expectRightParen(open)
        // The group's span includes its parentheses, so `(-1)!` underlines from the `(`.
        return inner.withSpan(open.span.first..close.span.last)
    }

    private fun Ast.withSpan(span: IntRange): Ast = when (this) {
        is Ast.NumberLiteral -> copy(span = span)
        is Ast.Identifier -> copy(span = span)
        is Ast.LineReference -> copy(span = span)
        is Ast.Unary -> copy(span = span)
        is Ast.Binary -> copy(span = span)
        is Ast.Postfix -> copy(span = span)
        is Ast.Call -> copy(span = span)
        is Ast.Conversion -> copy(span = span)
        is Ast.Assignment -> copy(span = span)
    }

    private fun expectRightParen(open: Token): Token {
        val token = peek()
        if (token.type != TokenType.RIGHT_PAREN) {
            throw ParseFailure(CalcError.UnbalancedParen(missing = 1, span = open.span))
        }
        return advance()
    }

    private fun expectEnd() {
        val token = peek()
        if (token.type != TokenType.END) fail(token)
    }

    /**
     * `in` is the inch unit only in unit position — directly after a value with nothing that
     * could start an expression behind it. `5 in` is five inches; `5 cm in mm` is a
     * conversion; `5 in 3` is a conversion whose target `3` is not a unit, and therefore a
     * syntax error on `3` (`docs/GRAMMAR.md` §3.3).
     */
    private fun isUnitPositionIn(token: Token): Boolean =
        token.text == INCH && !startsPrimary(peek(1))

    private fun implicitMultiply(left: Ast, right: Ast): Ast =
        Ast.Binary(BinaryOperator.IMPLICIT_MULTIPLY, left, right, left.span.first..right.span.last)

    private fun requireUnitExpression(target: Ast) {
        if (!isUnitExpression(target)) fail(target.span)
    }

    private fun isUnitExpression(node: Ast): Boolean = when (node) {
        is Ast.Identifier -> true
        is Ast.Binary -> when (node.operator) {
            BinaryOperator.MULTIPLY, BinaryOperator.DIVIDE, BinaryOperator.IMPLICIT_MULTIPLY ->
                isUnitExpression(node.left) && isUnitExpression(node.right)

            BinaryOperator.POWER -> isUnitExpression(node.left) && node.right is Ast.NumberLiteral
            else -> false
        }

        is Ast.Postfix -> node.operator == PostfixOperator.DEGREE
        else -> false
    }

    private fun infixBindingPower(token: Token): Int? = when (token.type) {
        TokenType.PLUS, TokenType.MINUS -> ADDITIVE
        TokenType.STAR, TokenType.SLASH -> MULTIPLICATIVE
        TokenType.CARET -> POWER
        TokenType.KEYWORD -> when (token.text) {
            in CONVERSION_KEYWORDS -> CONVERSION
            MOD, OF -> MULTIPLICATIVE
            else -> null
        }

        else -> null
    }

    private fun binaryOperator(token: Token): BinaryOperator = when (token.type) {
        TokenType.PLUS -> BinaryOperator.ADD
        TokenType.MINUS -> BinaryOperator.SUBTRACT
        TokenType.STAR -> BinaryOperator.MULTIPLY
        TokenType.SLASH -> BinaryOperator.DIVIDE
        TokenType.KEYWORD if token.text == MOD -> BinaryOperator.MODULO
        TokenType.KEYWORD if token.text == OF -> BinaryOperator.OF
        else -> fail(token)
    }

    private fun startsPrimary(token: Token): Boolean = when (token.type) {
        TokenType.NUMBER, TokenType.IDENTIFIER, TokenType.LINE_REF, TokenType.LEFT_PAREN -> true
        else -> false
    }

    private fun peek(offset: Int = 0): Token = tokens[minOf(position + offset, tokens.lastIndex)]

    private fun advance(): Token = peek().also { if (position < tokens.lastIndex) position++ }

    private fun fail(token: Token): Nothing =
        throw ParseFailure(CalcError.Syntax(token.text, token.span))

    private fun fail(span: IntRange): Nothing =
        throw ParseFailure(CalcError.Syntax(textAt(span), span))

    private fun textAt(span: IntRange): String =
        tokens.firstOrNull { it.span.first == span.first }?.text.orEmpty()

    private class ParseFailure(val error: CalcError) : RuntimeException(null, null, false, false)

    private companion object {
        const val CONVERSION = 10
        const val ADDITIVE = 20
        const val MULTIPLICATIVE = 30
        const val UNARY = 50
        const val POWER = 60

        const val MAX_DEPTH = 128
        const val INCH = "in"
        const val MOD = "mod"
        const val OF = "of"
        val CONVERSION_KEYWORDS = setOf("to", "in", "as")
    }
}
