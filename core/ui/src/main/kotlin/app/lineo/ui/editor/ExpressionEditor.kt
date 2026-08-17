package app.lineo.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.lineo.engine.CalcError
import app.lineo.ui.R
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.RoleColors
import app.lineo.ui.theme.asExpression

/**
 * One line of the notepad: the expression, its result, and what is wrong with it.
 *
 * Reads everything from [state] and writes nothing but through it, so the same line
 * behaves identically whether it was typed on the keypad, on the system keyboard, or
 * filled in by a suggestion chip.
 *
 * Nothing is rendered below the expression while the line is merely unfinished. That is
 * the whole point of `EditorEvaluation.Unfinished`: `5 +` is not a mistake, it is a
 * sentence the user has not got to the end of.
 */
@Composable
fun ExpressionEditor(
    state: EditorState,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val editor = RoleColors.of(LineoRole.Editor)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(editor.container)
            .padding(LineoDimens.EditorPadding),
        // The whole stack reads from the right, as the reference design does: a result
        // lines up with the expression it came from digit for digit.
        horizontalAlignment = Alignment.End,
    ) {
        ExpressionField(state = state, focusRequester = focusRequester)
        when (val evaluation = state.evaluation) {
            EditorEvaluation.Empty, EditorEvaluation.Unfinished -> Unit
            is EditorEvaluation.Result -> ResultLine(evaluation)
            is EditorEvaluation.Failure -> FailureLines(state = state, error = evaluation.error)
        }
    }
}

@Composable
private fun ExpressionField(state: EditorState, focusRequester: FocusRequester?) {
    val editor = RoleColors.of(LineoRole.Editor)
    val underlineColour = RoleColors.of(LineoRole.ErrorUnderline).container
    val span = (state.evaluation as? EditorEvaluation.Failure)?.error?.span

    BasicTextField(
        value = TextFieldValue(text = state.text, selection = TextRange(state.caret)),
        onValueChange = { state.setText(it.text, it.selection.start) },
        modifier = Modifier
            .fillMaxWidth()
            .let { if (focusRequester == null) it else it.focusRequester(focusRequester) },
        textStyle = MaterialTheme.typography.displayLarge
            .asExpression()
            .copy(color = editor.content, textAlign = TextAlign.End),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = errorSpanTransformation(span, underlineColour),
    )
}

/** The computed value, muted, directly under the expression it belongs to. */
@Composable
private fun ResultLine(result: EditorEvaluation.Result) {
    // canonicalString is locale-free. docs/CONVENTIONS.md §1 puts display formatting —
    // grouping and the locale separator — at this boundary, and that formatter does not
    // exist yet; it is recorded in TASKS.md as its own task rather than improvised here.
    Text(
        text = result.value.canonicalString(),
        style = MaterialTheme.typography.displayMedium.asExpression(),
        color = RoleColors.of(LineoRole.Result).content,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * The message, and the fix if the engine has one to offer.
 *
 * §10 forbids colour alone, so the underline in the field above is never the whole signal:
 * there is always a sentence here saying what happened.
 */
@Composable
private fun FailureLines(state: EditorState, error: CalcError) {
    val message = RoleColors.of(LineoRole.ErrorMessage)
    Text(
        text = calcErrorMessage(error),
        style = MaterialTheme.typography.bodySmall,
        color = message.content,
        modifier = Modifier
            .padding(top = LineoDimens.Grid)
            .clip(RoundedCornerShape(MessageCornerRadius))
            .background(message.container)
            .padding(horizontal = LineoDimens.Grid, vertical = LineoDimens.Grid),
    )
    val suggestion = (error as? CalcError.UnknownIdentifier)?.takeIf { it.suggestion != null }
    if (suggestion != null) {
        FixChip(label = requireNotNull(suggestion.suggestion), onFix = { state.applySuggestion(suggestion) })
    }
}

/**
 * Tap to replace the name the engine did not recognise with the one it thinks was meant.
 *
 * A chip rather than an automatic correction: the engine's nearest match is a guess, and a
 * calculator that silently rewrote what someone typed would be worse than one that asks.
 */
@Composable
private fun FixChip(label: String, onFix: () -> Unit) {
    val chip = RoleColors.of(LineoRole.SuggestionChip)
    Text(
        text = stringResource(R.string.editor_fix_to, label),
        style = MaterialTheme.typography.labelLarge,
        color = chip.content,
        modifier = Modifier
            .padding(top = LineoDimens.Grid)
            .clip(RoundedCornerShape(ChipCornerRadius))
            .background(chip.container)
            .clickable(onClick = onFix)
            .padding(horizontal = LineoDimens.LineGap, vertical = LineoDimens.Grid),
    )
}

/**
 * Marks [span] as the part that is wrong.
 *
 * The offsets are unchanged, so the mapping is the identity one and the caret keeps
 * landing where the user put it. The span is coerced because it describes the text the
 * engine last saw, and the user may have typed since.
 */
private fun errorSpanTransformation(span: IntRange?, colour: androidx.compose.ui.graphics.Color) =
    VisualTransformation { original ->
        if (span == null) {
            TransformedText(original, OffsetMapping.Identity)
        } else {
            val start = span.first.coerceIn(0, original.length)
            val end = (span.last + 1).coerceIn(start, original.length)
            TransformedText(original.underlined(start, end, colour), OffsetMapping.Identity)
        }
    }

private fun AnnotatedString.underlined(start: Int, end: Int, colour: androidx.compose.ui.graphics.Color) =
    buildAnnotatedString {
        append(this@underlined)
        // Colour and rule together. The rule alone would be invisible to anyone who cannot
        // see a two-pixel line; the colour alone would break §10's ban on colour-only
        // meaning. error on surface measures 6.13:1, above the AA minimum for text.
        addStyle(SpanStyle(color = colour, textDecoration = TextDecoration.Underline), start, end)
    }

private val MessageCornerRadius = 8.dp
private val ChipCornerRadius = 16.dp
