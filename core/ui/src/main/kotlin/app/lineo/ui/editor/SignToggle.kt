package app.lineo.ui.editor

/**
 * Where the sign of the number at [caret] should be flipped, and how.
 *
 * Separated from [EditorState] because it is the only part of the `±` key that is worth
 * arguing about, and the only part worth testing exhaustively. Given a line and a caret it
 * returns the text and caret that should replace them, and nothing else in the editor has
 * to know how a number is recognised.
 */
internal data class SignToggle(val text: String, val caret: Int)

/**
 * Flips the sign of the numeric literal the caret sits in or just after.
 *
 * The rules, in the order they are tried:
 *
 * 1. Scan back from the caret over digits and the decimal separator. That run is the
 *    number the user means — the one they were last typing.
 * 2. If a `-` sits immediately before it, and that `-` is a *sign* rather than a
 *    subtraction, remove it. A `-` is a sign when what precedes it is nothing, an opening
 *    bracket, or another operator: `5 * -3` negates, `5 - 3` subtracts.
 * 3. Otherwise insert a `-` in front of the number.
 * 4. With no number before the caret at all, insert a `-` at the caret, so `±` can start a
 *    negative number on an empty line rather than doing nothing.
 *
 * Step 2 is the whole reason this is not `InsertText("-")`. Pressing `±` twice has to
 * leave the line as it was found, and on `5 - 3` it must not turn a subtraction into a
 * mangled `5 3`.
 */
internal fun toggleSign(text: String, caret: Int, decimalSeparator: Char): SignToggle {
    val at = caret.coerceIn(0, text.length)
    var start = at
    while (start > 0 && (text[start - 1].isDigit() || text[start - 1] == decimalSeparator)) {
        start--
    }

    if (start == at) {
        // No number ends here; start one.
        return SignToggle(text.substring(0, at) + "-" + text.substring(at), at + 1)
    }

    val hasSign = start > 0 && text[start - 1] == '-' && isSignPosition(text, start - 1)
    return if (hasSign) {
        SignToggle(text.removeRange(start - 1, start), at - 1)
    } else {
        SignToggle(text.substring(0, start) + "-" + text.substring(start), at + 1)
    }
}

/**
 * Whether the `-` at [index] negates what follows rather than subtracting from what
 * precedes.
 *
 * True at the start of the line, and after an opening bracket or another operator. This is
 * the same question the parser answers for unary minus (`docs/GRAMMAR.md` §1), asked here
 * on raw text because the editor has no AST while the user is mid-line.
 */
private fun isSignPosition(text: String, index: Int): Boolean {
    val before = text.take(index).trimEnd()
    return before.isEmpty() || before.last() in SIGN_PRECEDERS
}

private const val SIGN_PRECEDERS = "+-*/^(,;"
