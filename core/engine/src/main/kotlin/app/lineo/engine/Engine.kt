package app.lineo.engine

/**
 * Entry point of the expression engine.
 *
 * The pipeline is `String → Lexer → Token[] → Parser → Ast → Evaluator →
 * CalcResult<Quantity>` (`docs/ARCHITECTURE.md` §2). Nothing here throws on bad user
 * input; every failure is a value.
 */
public object Engine
