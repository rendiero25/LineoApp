package app.lineo.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection

/**
 * Marks a style as carrying a mathematical expression or a number rather than prose.
 *
 * An expression is left to right in every locale — `1 234,5 + 67,89` is written that way
 * in Arabic and Hebrew too. Left to itself the bidi algorithm treats each run of digits as
 * a neutral island inside the surrounding right-to-left paragraph and reorders them, so
 * `1 234,5 + 67,89` renders as `67,89 + 234,5 1`: the same characters, a different sum.
 * Forcing [TextDirection.Ltr] on the paragraph keeps the operands in the order they were
 * typed while the layout around them still mirrors.
 *
 * Apply it to any text that is a number, an expression, a unit, or a result. Do not apply
 * it to prose — an error message, a label, a title — which must follow the locale.
 */
fun TextStyle.asExpression(): TextStyle = copy(textDirection = TextDirection.Ltr)
