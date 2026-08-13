package app.lineo.engine.lexer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * Locale behaviour of `docs/CONVENTIONS.md` §2, §3, §7 and the number rules of
 * `docs/GRAMMAR.md` §3.2 and §3.4, across the four locales named in TASKS.md P0-06.
 */
class LexerTest {

    @Test
    fun `en-US reads a dot decimal and ignores comma grouping`() {
        val tokens = lex("1,234.5 + 2", "en-US")

        assertEquals("1234.5", tokens.first().number?.toPlainString())
        assertEquals(listOf(TokenType.NUMBER, TokenType.PLUS, TokenType.NUMBER, TokenType.END), tokens.types())
    }

    @Test
    fun `en-US keeps the comma as an argument separator when it is not grouping`() {
        val tokens = lex("max(1,5)", "en-US")

        assertEquals(
            listOf(
                TokenType.IDENTIFIER,
                TokenType.LEFT_PAREN,
                TokenType.NUMBER,
                TokenType.ARG_SEPARATOR,
                TokenType.NUMBER,
                TokenType.RIGHT_PAREN,
                TokenType.END,
            ),
            tokens.types(),
        )
    }

    @Test
    fun `id-ID reads a comma decimal and a dot grouping`() {
        val tokens = lex("1.234,5", "id-ID")

        assertEquals("1234.5", tokens.first().number?.toPlainString())
    }

    @Test
    fun `id-ID uses a semicolon argument separator`() {
        val tokens = lex("max(1,5;2)", "id-ID")

        assertEquals(
            listOf(
                TokenType.IDENTIFIER,
                TokenType.LEFT_PAREN,
                TokenType.NUMBER,
                TokenType.ARG_SEPARATOR,
                TokenType.NUMBER,
                TokenType.RIGHT_PAREN,
                TokenType.END,
            ),
            tokens.types(),
        )
        assertEquals("1.5", tokens.first { it.type == TokenType.NUMBER }.number?.toPlainString())
    }

    @Test
    fun `de-DE reads a comma decimal`() {
        val tokens = lex("2.500,75", "de-DE")

        assertEquals("2500.75", tokens.first().number?.toPlainString())
    }

    @Test
    fun `hi-IN reads Indian grouping`() {
        val tokens = lex("12,34,567", "hi-IN")

        assertEquals("1234567", tokens.first().number?.toPlainString())
    }

    @Test
    fun `magnitude suffixes are locale gated`() {
        assertEquals("2000", lex("2k", "en-US").first().number?.toPlainString())
        assertEquals("2000", lex("2rb", "id-ID").first().number?.toPlainString())
        assertEquals("3000000", lex("3jt", "id-ID").first().number?.toPlainString())
        assertEquals("200000", lex("2lakh", "hi-IN").first().number?.toPlainString())
        assertEquals("2000", lex("2mil", "de-DE").first().number?.toPlainString())
    }

    @Test
    fun `a suffix that is not in the locale set stays an identifier`() {
        val tokens = lex("2rb", "en-US")

        assertEquals("2", tokens.first().number?.toPlainString())
        assertEquals(TokenType.IDENTIFIER, tokens[1].type)
    }

    @Test
    fun `a variable in scope beats a magnitude suffix`() {
        val tokens = lex("2k", "en-US", defined = setOf("k"))

        assertEquals("2", tokens.first().number?.toPlainString())
        assertEquals(TokenType.IDENTIFIER, tokens[1].type)
    }

    @Test
    fun `a unit symbol starting with a suffix letter is not a suffix`() {
        val tokens = lex("2km", "en-US")

        assertEquals("2", tokens.first().number?.toPlainString())
        assertEquals("km", tokens[1].text)
    }

    @Test
    fun `scientific notation is part of the number`() {
        assertEquals("100000", lex("1e5", "en-US").first().number?.toPlainString())
        assertEquals("0.002", lex("2E-3", "en-US").first().number?.toPlainString())
    }

    @Test
    fun `a bare e after digits is implicit multiplication, not an exponent`() {
        val tokens = lex("2e", "en-US")

        assertEquals("2", tokens.first().number?.toPlainString())
        assertEquals(TokenType.IDENTIFIER, tokens[1].type)
        assertEquals("e", tokens[1].text)
    }

    @Test
    fun `every token carries its source span`() {
        val tokens = lex("12 + 345", "en-US")

        assertEquals(0..1, tokens[0].span)
        assertEquals(3..3, tokens[1].span)
        assertEquals(5..7, tokens[2].span)
    }

    @Test
    fun `reserved words are keywords, not identifiers`() {
        val tokens = lex("5 km to mi", "en-US")

        assertEquals(TokenType.KEYWORD, tokens[2].type)
        assertEquals(TokenType.IDENTIFIER, tokens[3].type)
    }

    @Test
    fun `line references lex in both forms`() {
        assertEquals(3, lex("line3", "en-US").first().lineNumber)
        assertEquals(3, lex("@3", "en-US").first().lineNumber)
        assertEquals(TokenType.LINE_REF, lex("@3", "en-US").first().type)
    }

    @Test
    fun `comments run to the end of the line`() {
        val tokens = lex("2 + 3 // adds up", "en-US")

        assertEquals(listOf(TokenType.NUMBER, TokenType.PLUS, TokenType.NUMBER, TokenType.END), tokens.types())
    }

    @Test
    fun `unknown characters become unknown tokens rather than exceptions`() {
        val tokens = lex("2 § 3", "en-US")

        assertTrue(tokens.any { it.type == TokenType.UNKNOWN })
    }

    @Test
    fun `postfix symbols are their own tokens`() {
        val tokens = lex("50% 3! 90°", "en-US")

        assertTrue(tokens.types().containsAll(listOf(TokenType.PERCENT, TokenType.BANG, TokenType.DEGREE)))
    }

    private fun lex(source: String, tag: String, defined: Set<String> = emptySet()): List<Token> =
        Lexer(NumberFormatProfile.forLocale(Locale.forLanguageTag(tag)), defined).tokenize(source)

    private fun List<Token>.types(): List<TokenType> = map { it.type }
}
