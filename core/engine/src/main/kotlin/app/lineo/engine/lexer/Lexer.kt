package app.lineo.engine.lexer

import java.math.BigDecimal
import java.math.MathContext

/**
 * Turns source text into locale-free tokens.
 *
 * The lexer is the only place that knows about locale (`docs/ARCHITECTURE.md` §2): decimal
 * separator, argument separator, grouping characters and magnitude suffixes all come from
 * [NumberFormatProfile]. Everything downstream sees canonical [BigDecimal] values.
 *
 * It never throws. A character it does not understand becomes [TokenType.UNKNOWN], and the
 * parser reports it as a syntax error at the right span.
 *
 * [definedNames] are the identifiers already in scope. They matter because a variable wins
 * over a magnitude suffix: after `k = 3`, `2k` is `6`, not `2000` (`docs/GRAMMAR.md` §3.2).
 */
class Lexer(
    private val profile: NumberFormatProfile,
    private val definedNames: Set<String> = emptySet(),
) {
    fun tokenize(source: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var index = 0

        while (index < source.length) {
            val char = source[index]
            when {
                char.isWhitespace() -> index++

                isCommentStart(source, index) -> index = source.length

                char.isDigit() || isDecimalStart(source, index) -> {
                    val token = readNumber(source, index)
                    tokens += token
                    index = token.span.last + 1
                }

                char == LINE_REF_MARKER -> {
                    val token = readLineRef(source, index)
                    tokens += token
                    index = token.span.last + 1
                }

                char.isLetter() || char == UNDERSCORE -> {
                    val token = readWord(source, index)
                    tokens += token
                    index = token.span.last + 1
                }

                else -> {
                    val token = readSymbol(source, index)
                    tokens += token
                    index = token.span.last + 1
                }
            }
        }

        tokens += Token(TokenType.END, "", source.length..source.length)
        return tokens
    }

    private fun isCommentStart(source: String, index: Int): Boolean =
        source[index] == '/' && index + 1 < source.length && source[index + 1] == '/'

    private fun isDecimalStart(source: String, index: Int): Boolean =
        source[index] == profile.decimalSeparator &&
            index + 1 < source.length &&
            source[index + 1].isDigit()

    @Suppress("CyclomaticComplexMethod")
    private fun readNumber(source: String, start: Int): Token {
        val digits = StringBuilder()
        var index = start
        var seenDecimal = false

        while (index < source.length) {
            val char = source[index]
            when {
                char.isDigit() -> {
                    digits.append(char)
                    index++
                }

                char == profile.decimalSeparator && !seenDecimal && index + 1 < source.length &&
                    source[index + 1].isDigit() -> {
                    digits.append('.')
                    seenDecimal = true
                    index++
                }

                isGroupingHere(source, index) -> index++

                else -> break
            }
        }

        index = readExponent(source, index, digits)

        val multiplier = readSuffix(source, index)
        if (multiplier != null) index += multiplier.first.length

        val base = digits.toString().toBigDecimalOrZero()
        val value = if (multiplier == null) base else base.multiply(multiplier.second, MathContext.DECIMAL128)

        return Token(
            type = TokenType.NUMBER,
            text = source.substring(start, index),
            span = start until index,
            number = value,
        )
    }

    /** Scientific notation, `docs/GRAMMAR.md` §3.4: `1e5`, `2E-3`. A bare `e` is not one. */
    private fun readExponent(source: String, index: Int, digits: StringBuilder): Int {
        if (index >= source.length) return index
        if (source[index] != 'e' && source[index] != 'E') return index

        var cursor = index + 1
        val sign = if (cursor < source.length && (source[cursor] == '+' || source[cursor] == '-')) {
            source[cursor++]
        } else {
            null
        }
        if (cursor >= source.length || !source[cursor].isDigit()) return index

        val exponent = StringBuilder()
        while (cursor < source.length && source[cursor].isDigit()) {
            exponent.append(source[cursor])
            cursor++
        }

        digits.append('E')
        if (sign != null) digits.append(sign)
        digits.append(exponent)
        return cursor
    }

    /**
     * A magnitude suffix binds only when it directly follows the digits, is in the locale's
     * set, and no variable of that name is in scope (`docs/GRAMMAR.md` §3.2).
     */
    private fun readSuffix(source: String, index: Int): Pair<String, BigDecimal>? {
        if (index >= source.length || !source[index].isLetter()) return null

        var end = index
        while (end < source.length && source[end].isLetter()) end++
        val word = source.substring(index, end)
        if (word in definedNames) return null

        val multiplier = profile.magnitudeSuffixes.entries
            .firstOrNull { (suffix, _) -> word.equals(suffix, ignoreCase = true) }
            ?: return null

        return word to multiplier.value
    }

    /**
     * Grouping separators are ignored entirely (`docs/CONVENTIONS.md` §2). Where the
     * grouping character doubles as the argument separator, only a full group of digits
     * counts as grouping, so `1,234` is a number while `max(1,5)` is two arguments.
     */
    private fun isGroupingHere(source: String, index: Int): Boolean {
        val char = source[index]
        if (!profile.isGroupingCharacter(char)) return false

        val digitsAfter = countDigits(source, index + 1)
        if (digitsAfter == 0) return false

        val ambiguous = profile.groupingIsAmbiguous && char == profile.groupingSeparator
        return !ambiguous || digitsAfter in profile.groupSizes
    }

    private fun countDigits(source: String, from: Int): Int {
        var cursor = from
        while (cursor < source.length && source[cursor].isDigit()) cursor++
        return cursor - from
    }

    /** `@3` — the short form of a line reference (`docs/GRAMMAR.md` §3.7). */
    private fun readLineRef(source: String, start: Int): Token {
        var index = start + 1
        while (index < source.length && source[index].isDigit()) index++
        val text = source.substring(start, index)
        val number = text.drop(1).toIntOrNull()

        return if (number == null) {
            Token(TokenType.UNKNOWN, text, start..start)
        } else {
            Token(TokenType.LINE_REF, text, start until index, lineNumber = number)
        }
    }

    private fun readWord(source: String, start: Int): Token {
        var index = start
        while (index < source.length && (source[index].isLetterOrDigit() || source[index] == UNDERSCORE)) index++
        val text = source.substring(start, index)
        val span = start until index

        val lineRef = LINE_PREFIX_PATTERN.matchEntire(text)
        return when {
            lineRef != null -> Token(
                type = TokenType.LINE_REF,
                text = text,
                span = span,
                lineNumber = lineRef.groupValues[1].toInt(),
            )

            text in RESERVED_WORDS -> Token(TokenType.KEYWORD, text, span)

            else -> Token(TokenType.IDENTIFIER, text, span)
        }
    }

    private fun readSymbol(source: String, start: Int): Token {
        val char = source[start]
        val span = start..start
        val type = when (char) {
            '+' -> TokenType.PLUS
            '-', '−' -> TokenType.MINUS
            '*', '×', '⋅' -> TokenType.STAR
            '/', '÷', '∕' -> TokenType.SLASH
            '^' -> TokenType.CARET
            '%' -> TokenType.PERCENT
            '!' -> TokenType.BANG
            '°' -> TokenType.DEGREE
            '(', '[' -> TokenType.LEFT_PAREN
            ')', ']' -> TokenType.RIGHT_PAREN
            '=' -> TokenType.ASSIGN
            profile.argumentSeparator -> TokenType.ARG_SEPARATOR
            else -> TokenType.UNKNOWN
        }
        return Token(type, char.toString(), span)
    }

    private fun String.toBigDecimalOrZero(): BigDecimal =
        if (isEmpty() || this == ".") BigDecimal.ZERO else BigDecimal(this, MathContext.DECIMAL128)

    private companion object {
        const val LINE_REF_MARKER = '@'
        const val UNDERSCORE = '_'
        val LINE_PREFIX_PATTERN = Regex("line(\\d+)")
    }
}
