package app.lineo.ui.editor

import app.lineo.engine.CalcError

/**
 * Whether [error] means "not finished yet" rather than "wrong".
 *
 * The distinction cannot come from the error type alone. `CalcError.Syntax` covers both
 * `5 +`, which the next keystroke fixes, and `5 + + 3`, which it does not. What separates
 * them is *where* the failure is: at the end of what has been typed, or inside it.
 *
 * - An unbalanced bracket is always unfinished. `(1 + 2` becomes valid by typing `)`, and
 *   there is no way to have typed a closing bracket that is still missing.
 * - A syntax error is unfinished when it sits at the end of the trimmed line. `5 +` fails
 *   at position 2, which is the end; `5 + + 3` fails at position 4, which is not.
 *
 * Everything else — an unknown name, a unit mismatch, a division by zero — is a real
 * mistake the moment it appears, and typing more will not undo it.
 */
internal fun isUnfinished(error: CalcError, source: String): Boolean = when (error) {
    is CalcError.UnbalancedParen -> error.missing > 0
    is CalcError.Syntax -> error.span.first >= source.trimEnd().length
    else -> false
}
