package app.lineo.engine.golden

import app.lineo.engine.AngleMode
import java.util.Locale

/**
 * One line of a golden file, in the format fixed by `docs/GRAMMAR.md` §5:
 *
 * ```
 * # comment
 * locale=en-US
 * angle=DEG
 * 2+3*4                       | 14
 * 1/0                         | !DivisionByZero
 * sni(1)                      | !UnknownIdentifier@0..2
 * ```
 *
 * [lineNumber] is the 1-based line in [file], so a failure points at the golden line that
 * has to be looked at — never at a line of test code.
 */
data class GoldenCase(
    val file: String,
    val lineNumber: Int,
    val locale: Locale,
    val angleMode: AngleMode,
    val input: String,
    val expectation: Expectation,
) {
    /** `file:line` — the location a failure report leads with. */
    val location: String get() = "$file:$lineNumber"
}

/** What a golden line asserts about the result. */
sealed interface Expectation {
    /**
     * The canonical rendering of a successful result, e.g. `14` or `5.3 km`.
     *
     * Expectations are locale-free on purpose: `locale=` selects how the *input* is read
     * (`docs/CONVENTIONS.md` §1 — internal representation is canonical), so a golden line
     * asserts engine behaviour rather than display formatting.
     */
    data class Value(val canonical: String) : Expectation

    /** An error of [type], optionally with the exact source [span] it must point at. */
    data class Error(val type: String, val span: IntRange?) : Expectation
}
