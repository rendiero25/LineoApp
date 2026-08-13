package app.lineo.engine.parser

import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.lexer.Lexer
import app.lineo.engine.lexer.NumberFormatProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * Structure of the parse, row by row against `docs/CONVENTIONS.md` §4 and the ambiguity
 * decisions of `docs/GRAMMAR.md` §3. Values are checked by the golden suite once the
 * evaluator lands (P0-08); what matters here is that the tree has the right shape.
 */
class ParserTest {

    @Test
    fun `unary minus binds looser than exponentiation`() {
        val ast = parse("-2^2").tree()

        assertTrue(ast is Ast.Unary, ast.toString())
        assertEquals(BinaryOperator.POWER, ((ast as Ast.Unary).operand as Ast.Binary).operator)
    }

    @Test
    fun `exponentiation is right associative`() {
        val ast = parse("2^3^2").tree() as Ast.Binary

        assertEquals(BinaryOperator.POWER, ast.operator)
        assertEquals(BinaryOperator.POWER, (ast.right as Ast.Binary).operator)
    }

    @Test
    fun `implicit multiplication evaluates at the same level as explicit`() {
        val ast = parse("6/2(1+3)").tree() as Ast.Binary

        assertEquals(BinaryOperator.IMPLICIT_MULTIPLY, ast.operator)
        assertEquals(BinaryOperator.DIVIDE, (ast.left as Ast.Binary).operator)
    }

    @Test
    fun `implicit multiplication between parentheses is allowed`() {
        val ast = parse("(1+2)(3+4)").tree() as Ast.Binary

        assertEquals(BinaryOperator.IMPLICIT_MULTIPLY, ast.operator)
    }

    @Test
    fun `percent is postfix and keeps its position in the tree`() {
        val ast = parse("100 + 10%").tree() as Ast.Binary

        assertEquals(BinaryOperator.ADD, ast.operator)
        assertEquals(PostfixOperator.PERCENT, (ast.right as Ast.Postfix).operator)
    }

    @Test
    fun `of is a keyword operator`() {
        val ast = parse("50% of 80").tree() as Ast.Binary

        assertEquals(BinaryOperator.OF, ast.operator)
        assertEquals(PostfixOperator.PERCENT, (ast.left as Ast.Postfix).operator)
    }

    @Test
    fun `a single argument builtin may drop its parentheses`() {
        val ast = parse("sin 30 + 1").tree() as Ast.Binary

        assertEquals(BinaryOperator.ADD, ast.operator)
        assertEquals("sin", (ast.left as Ast.Call).name)
    }

    @Test
    fun `a parenthesis free call takes the whole implicit product`() {
        val ast = parse("sin 2x").tree() as Ast.Call

        assertEquals("sin", ast.name)
        assertEquals(BinaryOperator.IMPLICIT_MULTIPLY, (ast.arguments.single() as Ast.Binary).operator)
    }

    @Test
    fun `an unknown name is never a parenthesis free call`() {
        val ast = parse("foo 2").tree() as Ast.Binary

        assertEquals(BinaryOperator.IMPLICIT_MULTIPLY, ast.operator)
    }

    @Test
    fun `calls with several arguments use the locale argument separator`() {
        val ast = parse("max(1, 5)").tree() as Ast.Call

        assertEquals(2, ast.arguments.size)
    }

    @Test
    fun `conversion parses a composite unit target`() {
        val ast = parse("100 km/h to m/s").tree() as Ast.Conversion

        assertEquals(BinaryOperator.DIVIDE, (ast.target as Ast.Binary).operator)
    }

    @Test
    fun `in directly after a value is the inch unit`() {
        val ast = parse("5 in").tree() as Ast.Binary

        assertEquals(BinaryOperator.IMPLICIT_MULTIPLY, ast.operator)
        assertEquals("in", (ast.right as Ast.Identifier).name)
    }

    @Test
    fun `in between two expressions is a conversion`() {
        val ast = parse("5 cm in mm").tree()

        assertTrue(ast is Ast.Conversion, ast.toString())
    }

    @Test
    fun `a conversion target that is not a unit is a syntax error on the target`() {
        val error = parse("5 in 3").error()

        assertTrue(error is CalcError.Syntax, error.toString())
        assertEquals(5..5, error?.span)
    }

    @Test
    fun `an unclosed parenthesis reports the opening one`() {
        val error = parse("(1+2").error()

        assertTrue(error is CalcError.UnbalancedParen, error.toString())
        assertEquals(0..0, error?.span)
    }

    @Test
    fun `incomplete input is a syntax error at the end`() {
        val error = parse("5 +").error()

        assertTrue(error is CalcError.Syntax, error.toString())
        assertEquals(3..3, error?.span)
    }

    @Test
    fun `postfix factorial and degree parse`() {
        assertEquals(PostfixOperator.FACTORIAL, (parse("5!").tree() as Ast.Postfix).operator)
        assertEquals(PostfixOperator.DEGREE, (parse("90°").tree() as Ast.Postfix).operator)
    }

    @Test
    fun `a labelled line is an assignment`() {
        val ast = parse("total = 2 + 3").tree() as Ast.Assignment

        assertEquals("total", ast.name)
        assertEquals(BinaryOperator.ADD, (ast.value as Ast.Binary).operator)
    }

    @Test
    fun `line references parse in both forms`() {
        assertEquals(3, (parse("line3 + 1").tree() as Ast.Binary).left.let { (it as Ast.LineReference).line })
        assertEquals(7, (parse("@7").tree() as Ast.LineReference).line)
    }

    @Test
    fun `mod is an operator at the multiplicative level`() {
        val ast = parse("10 mod 3 + 1").tree() as Ast.Binary

        assertEquals(BinaryOperator.ADD, ast.operator)
        assertEquals(BinaryOperator.MODULO, (ast.left as Ast.Binary).operator)
    }

    @Test
    fun `every node carries a span`() {
        val ast = parse("12 + 345").tree() as Ast.Binary

        assertEquals(0..7, ast.span)
        assertEquals(0..1, ast.left.span)
        assertEquals(5..7, ast.right.span)
    }

    private fun parse(source: String, tag: String = "en-US"): CalcResult<Ast> {
        val locale = Locale.forLanguageTag(tag)
        val tokens = Lexer(NumberFormatProfile.forLocale(locale)).tokenize(source)
        return Parser(tokens, knownFunctions = setOf("sin", "cos", "max")).parse()
    }

    private fun CalcResult<Ast>.tree(): Ast = requireNotNull(valueOrNull()) { "parse failed: ${errorOrNull()}" }

    private fun CalcResult<Ast>.error(): CalcError? = errorOrNull()
}
