package app.lineo.ui.a11y

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import app.lineo.engine.parser.Ast
import app.lineo.engine.parser.BinaryOperator
import app.lineo.engine.parser.PostfixOperator
import app.lineo.engine.parser.UnaryOperator
import app.lineo.ui.R

/**
 * An expression as TalkBack should say it: `2^3` is "2 to the power of 3", never
 * "2 caret 3" (`docs/CONVENTIONS.md` §8).
 *
 * Built from the **AST**, not from the source text, and that is the whole point. A reader
 * that walked the characters would have to guess what `^`, `%` and `!` mean in position —
 * and `%` alone is three different things in this grammar (`docs/GRAMMAR.md` §3.6). The
 * parser has already decided all of it, so the tree says what the user wrote.
 *
 * A line that does not parse has no tree, so it has nothing to say here: the caller falls
 * back to the raw text, which is what the user typed and is still being typed.
 */
object ExpressionSpeech {

    /**
     * Speaks [ast] in the words of [words].
     *
     * Brackets are spoken only where they are needed to hear the shape: an operand that
     * binds *looser* than the operator above it was parenthesised in the source and changes
     * the answer, so `(1+2)*3` is bracketed and `1+2*3` is not. Speaking every level would
     * bury a short expression in punctuation.
     */
    fun of(ast: Ast, words: SpeechWords): String = when (ast) {
        is Ast.NumberLiteral -> ast.value.toPlainString()
        is Ast.Identifier -> ast.name
        is Ast.LineReference -> words.lineReference(ast.line)
        is Ast.Unary -> unary(ast, words)
        is Ast.Binary -> binary(ast, words)
        is Ast.Postfix -> "${bracketed(ast.operand, POSTFIX_BINDING, words)} ${words.postfix(ast.operator)}"
        is Ast.Call -> call(ast, words)
        is Ast.Conversion -> "${of(ast.value, words)} ${words.to} ${of(ast.target, words)}"
        is Ast.Assignment -> "${ast.name} ${words.assigned} ${of(ast.value, words)}"
    }

    private fun unary(ast: Ast.Unary, words: SpeechWords): String = when (ast.operator) {
        UnaryOperator.NEGATE -> "${words.negative} ${bracketed(ast.operand, UNARY_BINDING, words)}"
        // Explicit `+` says nothing a reader needs: it is the value itself.
        UnaryOperator.PLUS -> of(ast.operand, words)
    }

    private fun binary(ast: Ast.Binary, words: SpeechWords): String {
        val binding = binding(ast.operator)
        val left = bracketed(ast.left, binding, words)
        val right = bracketed(ast.right, binding, words)
        // `5 km` is a quantity, and the parser reaches it as an implicit multiplication by a
        // name (`docs/GRAMMAR.md` §2). Saying "5 times km" would read the machinery out loud
        // rather than what the user wrote. `2(1+3)` keeps its "times": there the
        // multiplication is the point.
        val juxtaposed = ast.operator == BinaryOperator.IMPLICIT_MULTIPLY && ast.right is Ast.Identifier
        return if (juxtaposed) "$left $right" else "$left ${words.binary(ast.operator)} $right"
    }

    /**
     * "sin of 30", "max of 1 and 5" — the same shape whatever the arity, so a function this
     * build has never heard of still reads as a call rather than a name followed by loose
     * numbers.
     */
    private fun call(ast: Ast.Call, words: SpeechWords): String = if (ast.arguments.isEmpty()) {
        ast.name
    } else {
        "${ast.name} ${words.of} ${ast.arguments.joinToString(" ${words.and} ") { of(it, words) }}"
    }

    /** [ast] as an operand under something binding at [binding], bracketed only if it must be. */
    private fun bracketed(ast: Ast, binding: Int, words: SpeechWords): String {
        val spoken = of(ast, words)
        val own = bindingOf(ast)
        return if (own >= binding) spoken else "${words.openBracket} $spoken ${words.closeBracket}"
    }

    /** How tightly a node holds together. A leaf holds absolutely. */
    private fun bindingOf(ast: Ast): Int = when (ast) {
        is Ast.Binary -> binding(ast.operator)
        is Ast.Unary -> UNARY_BINDING
        is Ast.Conversion, is Ast.Assignment -> LOOSEST
        else -> LEAF
    }

    /**
     * Binding strength per operator, following the precedence table of `docs/GRAMMAR.md` §1.
     *
     * These are *not* the parser's numbers and must never be used to parse anything. They
     * decide one thing only: whether a bracket is worth saying out loud.
     */
    private fun binding(operator: BinaryOperator): Int = when (operator) {
        BinaryOperator.ADD, BinaryOperator.SUBTRACT -> ADDITIVE_BINDING
        BinaryOperator.MULTIPLY, BinaryOperator.DIVIDE, BinaryOperator.MODULO,
        BinaryOperator.IMPLICIT_MULTIPLY, BinaryOperator.OF,
        -> MULTIPLICATIVE_BINDING

        BinaryOperator.POWER -> POWER_BINDING
    }

    /** A whole line: nothing holds looser, so an operand of one is always worth bracketing. */
    private const val LOOSEST = 0
    private const val ADDITIVE_BINDING = 1
    private const val MULTIPLICATIVE_BINDING = 2
    private const val POWER_BINDING = 3
    private const val UNARY_BINDING = 4
    private const val POSTFIX_BINDING = 5

    /** A number or a name. Nothing inside it can need a bracket. */
    private const val LEAF = 6
}

/**
 * The words an expression is spoken in.
 *
 * A value passed in rather than resources read inside the walk, so the mapping from tree to
 * sentence is a pure function a test can assert without a `Context` — and so `en` is not
 * baked into it. Every string here is translatable.
 */
@Immutable
data class SpeechWords(
    val plus: String,
    val minus: String,
    val times: String,
    val dividedBy: String,
    val modulo: String,
    val toThePowerOf: String,
    val percentOf: String,
    val negative: String,
    val percent: String,
    val factorial: String,
    val degrees: String,
    val of: String,
    val and: String,
    val to: String,
    val assigned: String,
    val openBracket: String,
    val closeBracket: String,
    val line: String,
) {

    internal fun binary(operator: BinaryOperator): String = when (operator) {
        BinaryOperator.ADD -> plus
        BinaryOperator.SUBTRACT -> minus
        BinaryOperator.MULTIPLY, BinaryOperator.IMPLICIT_MULTIPLY -> times
        BinaryOperator.DIVIDE -> dividedBy
        BinaryOperator.MODULO -> modulo
        BinaryOperator.POWER -> toThePowerOf
        BinaryOperator.OF -> percentOf
    }

    internal fun postfix(operator: PostfixOperator): String = when (operator) {
        PostfixOperator.PERCENT -> percent
        PostfixOperator.FACTORIAL -> factorial
        PostfixOperator.DEGREE -> degrees
    }

    /** "line 3" — the same word the gutter shows, so the two agree out loud. */
    internal fun lineReference(ordinal: Int): String = "$line $ordinal"
}

/** The words of the current locale. */
@Composable
fun rememberSpeechWords(): SpeechWords = SpeechWords(
    plus = stringResource(R.string.speech_plus),
    minus = stringResource(R.string.speech_minus),
    times = stringResource(R.string.speech_times),
    dividedBy = stringResource(R.string.speech_divided_by),
    modulo = stringResource(R.string.speech_modulo),
    toThePowerOf = stringResource(R.string.speech_to_the_power_of),
    percentOf = stringResource(R.string.speech_percent_of),
    negative = stringResource(R.string.speech_negative),
    percent = stringResource(R.string.speech_percent),
    factorial = stringResource(R.string.speech_factorial),
    degrees = stringResource(R.string.speech_degrees),
    of = stringResource(R.string.speech_of),
    and = stringResource(R.string.speech_and),
    to = stringResource(R.string.speech_to),
    assigned = stringResource(R.string.speech_assigned),
    openBracket = stringResource(R.string.speech_open_bracket),
    closeBracket = stringResource(R.string.speech_close_bracket),
    line = stringResource(R.string.speech_line),
)
