package app.lineo.engine

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
    /**
     * Evaluates a single source line.
     *
     * Stub until P0-07 and P0-08 land: it reports the whole input as a syntax error, which
     * is enough for the golden harness and the fuzz test to exercise the contract.
     */
    @Suppress("UnusedParameter") // `context` is read once the evaluator lands in P0-08.
    fun evaluate(source: String, context: EvalContext = EvalContext()): CalcResult<Quantity> {
        val end = if (source.isEmpty()) 0 else source.length - 1
        return CalcError.Syntax(token = source, span = 0..end).err()
    }
}
