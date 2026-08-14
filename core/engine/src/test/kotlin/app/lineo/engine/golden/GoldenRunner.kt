package app.lineo.engine.golden

import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.EvalContext
import app.lineo.engine.Quantity

/**
 * Runs golden cases against an evaluator and describes what went wrong.
 *
 * The runner returns failures rather than throwing so that the harness can be tested on
 * its own — see `GoldenHarnessTest`.
 */
object GoldenRunner {
    /** Evaluates [case] and returns a failure description, or `null` when it passes. */
    fun run(
        case: GoldenCase,
        evaluate: (String, EvalContext) -> CalcResult<Quantity> = { source, context ->
            app.lineo.engine.Engine.evaluate(source, context)
        },
    ): String? {
        val context = EvalContext(locale = case.locale, angleMode = case.angleMode)
        val result = runCatching { evaluate(case.input, context) }
            .getOrElse { throwable ->
                return report(case, "the engine threw ${throwable::class.simpleName}: ${throwable.message}")
            }

        return when (val expectation = case.expectation) {
            is Expectation.Value -> checkValue(case, expectation, result)
            is Expectation.Error -> checkError(case, expectation, result)
        }
    }

    private fun checkValue(case: GoldenCase, expectation: Expectation.Value, result: CalcResult<Quantity>): String? =
        when (result) {
            is CalcResult.Ok -> {
                val actual = result.value.canonicalString()
                if (actual == expectation.canonical) {
                    null
                } else {
                    report(case, "expected ${expectation.canonical}, got $actual")
                }
            }

            is CalcResult.Err -> report(case, "expected ${expectation.canonical}, got ${describe(result.error)}")
        }

    private fun checkError(case: GoldenCase, expectation: Expectation.Error, result: CalcResult<Quantity>): String? =
        when (result) {
            is CalcResult.Ok ->
                report(case, "expected !${expectation.type}, got ${result.value.canonicalString()}")

            is CalcResult.Err -> {
                val actualType = errorName(result.error)
                val actualSpan = result.error.span
                when {
                    actualType != expectation.type ->
                        report(case, "expected !${expectation.type}, got ${describe(result.error)}")

                    expectation.span != null && actualSpan != expectation.span ->
                        report(
                            case,
                            "expected !${expectation.type} at ${spanText(expectation.span)}, " +
                                "got ${spanText(actualSpan)}",
                        )

                    else -> null
                }
            }
        }

    private fun report(case: GoldenCase, detail: String): String =
        "${case.location}: `${case.input}` — $detail"

    private fun describe(error: CalcError): String {
        val name = errorName(error)
        val span = error.span
        return if (span == null) "!$name" else "!$name${spanText(span)}"
    }

    private fun spanText(span: IntRange?): String =
        if (span == null) "no span" else "@${span.first}..${span.last}"

    /** The name used in golden files: the simple class name of the [CalcError] variant. */
    private fun errorName(error: CalcError): String = error::class.simpleName.orEmpty()
}
