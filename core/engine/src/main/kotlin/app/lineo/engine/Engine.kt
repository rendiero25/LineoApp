package app.lineo.engine

import app.lineo.engine.eval.Evaluator
import app.lineo.engine.lexer.Lexer
import app.lineo.engine.lexer.NumberFormatProfile
import app.lineo.engine.parser.Ast
import app.lineo.engine.parser.Parser

/**
 * Entry point of the expression engine.
 *
 * The pipeline is `String → Lexer → Token[] → Parser → Ast → Evaluator →
 * CalcResult<Quantity>` (`docs/ARCHITECTURE.md` §2).
 *
 * [evaluate] never throws, whatever the input. That contract is enforced by
 * `EngineFuzzTest` and is the foundation the rest of the app rests on
 * (`docs/GRAMMAR.md` §6).
 */
object Engine {
    /** Evaluates a single source line. */
    fun evaluate(source: String, context: EvalContext = EvalContext()): CalcResult<Quantity> =
        parse(source, context).flatMap { ast -> Evaluator(context).evaluate(ast) }

    /** Parses a single source line, exposed for the editor's syntax feedback. */
    fun parse(source: String, context: EvalContext = EvalContext()): CalcResult<Ast> {
        val profile = NumberFormatProfile.forLocale(context.locale)
        val tokens = Lexer(profile, definedNames = context.variables.keys).tokenize(source)
        return Parser(tokens, knownFunctions = context.functions.names).parse()
    }

    /** The name a labelled line defines, or `null` when the line is a bare expression. */
    fun assignedName(ast: Ast): String? = (ast as? Ast.Assignment)?.name
}
