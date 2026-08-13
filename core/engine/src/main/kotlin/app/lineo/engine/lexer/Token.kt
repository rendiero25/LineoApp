package app.lineo.engine.lexer

import java.math.BigDecimal

/**
 * A lexed token. Every token carries the [span] it came from so that an error can be
 * underlined at exactly the right characters (`docs/ARCHITECTURE.md` §3).
 */
data class Token(
    val type: TokenType,
    val text: String,
    val span: IntRange,
    val number: BigDecimal? = null,
    val lineNumber: Int? = null,
) {
    val isKeyword: Boolean get() = type == TokenType.KEYWORD
}

enum class TokenType {
    NUMBER,
    IDENTIFIER,

    /** `line3` or `@3` — a reference to a stable line id (`docs/GRAMMAR.md` §3.7). */
    LINE_REF,

    /** One of the reserved words of `docs/GRAMMAR.md` §4. */
    KEYWORD,

    PLUS,
    MINUS,
    STAR,
    SLASH,
    CARET,
    PERCENT,
    BANG,
    DEGREE,
    LEFT_PAREN,
    RIGHT_PAREN,
    ARG_SEPARATOR,
    ASSIGN,

    /** A character the engine does not understand. The parser turns it into a syntax error. */
    UNKNOWN,

    END,
}

/**
 * Reserved words, `docs/GRAMMAR.md` §4. They cannot be used as variable or formula names.
 *
 * `in` is special: it is the conversion operator, and only becomes the inch unit when it
 * sits in unit position (`docs/GRAMMAR.md` §3.3). The parser makes that call, not the lexer.
 */
val RESERVED_WORDS: Set<String> = setOf(
    "to", "in", "as", "of", "mod", "and", "or", "not", "true", "false", "line", "if", "else",
)
