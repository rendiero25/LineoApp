package app.lineo.notepad

import app.lineo.engine.LineId
import app.lineo.engine.lexer.Lexer
import app.lineo.engine.lexer.NumberFormatProfile
import app.lineo.engine.lexer.Token
import app.lineo.engine.lexer.TokenType
import java.util.Locale

/**
 * The boundary between what a line reference *is* and what it *looks like*.
 *
 * Stored text carries the stable line id: `line7` means the line whose [LineId] is 7, for as
 * long as that line exists. Displayed text carries the ordinal: the same reference reads as
 * `line3` when its target happens to be the third line, and reads as `line4` the moment a
 * line is inserted above it — without a character of stored text changing
 * (`docs/GRAMMAR.md` §3.7, `docs/ARCHITECTURE.md` §6).
 *
 * This is the same shape as the locale boundary of `docs/CONVENTIONS.md` §1: one canonical
 * representation inside, a presentation form at the edges, and exactly one place that
 * converts between them.
 *
 * References are found by lexing rather than by matching text, so the two forms of the token
 * (`line3` and `@3`) and the places a reference cannot appear are decided by the grammar and
 * not restated here. One consequence is deliberate: the lexer stops at a `//` comment, so a
 * reference written inside a comment is left exactly as typed. It is not evaluated either.
 */
object LineReferences {

    /**
     * Every line this text refers to, in the order the references appear.
     *
     * Reads stored text, so the numbers are ids. Duplicates are kept: `line1 + line1` refers
     * to one line twice, and a caller counting references wants to see both.
     */
    fun referencedIds(canonical: String): List<LineId> =
        lineRefTokens(canonical).mapNotNull { it.lineNumber?.let { number -> LineId(number.toLong()) } }

    /**
     * Rewrites stored text for display, turning each id into the ordinal it currently sits at.
     *
     * A reference whose target no longer exists becomes [MISSING_TARGET] rather than keeping
     * its id. Keeping it would be worse than losing it: `line7` would then be read as ordinal
     * 7, which is a different line's content, and the user would never be told.
     */
    fun toDisplay(canonical: String, ordinalOf: (LineId) -> Int?): String =
        rewrite(canonical) { id -> ordinalOf(LineId(id.toLong()))?.toLong() ?: MISSING_TARGET }

    /**
     * Rewrites what the user typed into stored text, binding each ordinal to the id that
     * occupies it now.
     *
     * An ordinal with no line behind it also becomes [MISSING_TARGET]. `line99` in a
     * four-line document is a mistake the user should see as a mistake, not a reference that
     * silently starts working when the document grows.
     */
    fun toCanonical(display: String, idAtOrdinal: (Int) -> LineId?): String =
        rewrite(display) { ordinal -> idAtOrdinal(ordinal)?.value ?: MISSING_TARGET }

    /**
     * The number written in place of a reference that points at nothing.
     *
     * Zero is safe because it is neither a valid id nor a valid ordinal — both count from
     * one. The engine resolves it like any other reference, finds nothing, and reports
     * `UnknownIdentifier("line0")` with the span of the token, which is exactly the
     * treatment a broken reference deserves.
     */
    const val MISSING_TARGET: Long = 0L

    private fun rewrite(text: String, map: (Int) -> Long): String {
        val tokens = lineRefTokens(text)
        if (tokens.isEmpty()) return text
        val rewritten = StringBuilder(text)
        // Back to front: replacing a token shifts every span after it, and none before it.
        tokens.asReversed().forEach { token ->
            val number = token.lineNumber ?: return@forEach
            val target = map(number)
            val replacement = when {
                token.text.startsWith(SHORT_FORM_MARKER) -> "$SHORT_FORM_MARKER$target"
                else -> "line$target"
            }
            rewritten.replace(token.span.first, token.span.last + 1, replacement)
        }
        return rewritten.toString()
    }

    private fun lineRefTokens(text: String): List<Token> =
        LEXER.tokenize(text).filter { it.type == TokenType.LINE_REF }

    /**
     * The locale a reference is read in.
     *
     * `line3` and `@3` are spelled the same everywhere — no locale moves the digits or the
     * keyword — so the profile only ever affects tokens this object throws away. Reading the
     * user's locale here would suggest it mattered.
     */
    private val LEXER = Lexer(NumberFormatProfile.forLocale(Locale.ROOT))

    private const val SHORT_FORM_MARKER = '@'
}
