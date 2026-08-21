package app.lineo.notepad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import app.lineo.engine.LineId
import app.lineo.ui.a11y.ExpressionSpeech
import app.lineo.ui.a11y.rememberSpeechWords
import app.lineo.ui.editor.calcErrorMessage
import app.lineo.ui.format.LocalQuantityFormat
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.RoleColors
import app.lineo.ui.theme.asExpression

/**
 * One line of the notepad: its number, what the user wrote, and what it came to.
 *
 * The number sits in a gutter of its own because a reference names it: `line3` means nothing
 * unless the user can see which line is three. It is display and never identity — the
 * document keeps that in [LineId] (`docs/GRAMMAR.md` §3.7).
 *
 * Only the focused line is a text field. Every other line is a `Text` that takes focus when
 * tapped, so a two-hundred-line document holds one field rather than two hundred, and the
 * caret can only ever be in the line the state says it is in. That is why [caret] is
 * nullable and there is no separate `focused` flag: a line either has the caret, at a
 * position, or it does not, and the two cannot be set to disagree.
 *
 * The expression and its result are aligned to the end and sit one step apart on the type
 * scale, so their digits line up column for column — the P0-14-2 decision, per line.
 */
@Composable
internal fun NotepadLineRow(
    line: NotepadLineUiState,
    caret: Int?,
    actions: NotepadLineActions,
    modifier: Modifier = Modifier,
    keyboardWanted: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = LineoDimens.MinTouchTarget)
            .padding(horizontal = LineoDimens.EditorPadding, vertical = LineoDimens.Grid),
        verticalAlignment = Alignment.Top,
    ) {
        LineNumber(ordinal = line.ordinal)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Expression(
                line = line,
                caret = caret,
                onFocus = actions.focus,
                onTextChange = actions.setText,
                onNewLine = actions.newLine,
                keyboardWanted = keyboardWanted,
            )
            Evaluation(
                line = line,
                ordinalOf = actions.ordinalOf,
                onApplySuggestion = actions.applySuggestion,
            )
        }
    }
}

/**
 * What one line can ask the document to do.
 *
 * Held together rather than passed one lambda at a time: they are one thing — this line,
 * edited — and a row that took six callbacks would grow a seventh the next time a line
 * learns a trick. [ordinalOf] is here for the same reason, being the one question the row
 * has to ask about a line that is not itself: which number a blocked line is waiting on.
 */
@Immutable
internal data class NotepadLineActions(
    val focus: () -> Unit,
    val setText: (String, Int) -> Unit,
    val newLine: () -> Unit,
    val applySuggestion: (CalcError.UnknownIdentifier) -> Unit,
    val ordinalOf: (LineId) -> Int?,
)

/** The ordinal, in a fixed-width gutter so every line's expression starts at the same place. */
@Composable
private fun LineNumber(ordinal: Int) {
    val description = stringResource(R.string.notepad_line_number_description, ordinal)
    Text(
        text = ordinal.toString(),
        style = MaterialTheme.typography.labelMedium,
        color = RoleColors.of(LineoRole.Result).content,
        modifier = Modifier
            .width(GutterWidth)
            .padding(top = LineoDimens.LineGap)
            .semantics { contentDescription = description },
    )
}

@Composable
private fun Expression(
    line: NotepadLineUiState,
    caret: Int?,
    onFocus: () -> Unit,
    onTextChange: (String, Int) -> Unit,
    onNewLine: () -> Unit,
    keyboardWanted: Boolean,
) {
    // The request is made here, by the composable that owns the field, and not by the screen.
    // A FocusRequester that is not attached throws when asked, and the screen cannot know
    // whether this line is currently composed — in a long document the focused line may be
    // scrolled far out of the viewport.
    val focusRequester = remember { FocusRequester() }
    // Not while being rendered by a preview or a screenshot test. Taking focus there starts a
    // text-input session, and layoutlib has no thread to give it: `Thread.setPosixNicenessInternal`
    // is missing, which crashes the render rather than failing a comparison. A picture has no
    // keyboard to raise anyway.
    val inspecting = LocalInspectionMode.current
    LaunchedEffect(caret != null, keyboardWanted, inspecting) {
        if (!inspecting && caret != null && keyboardWanted) focusRequester.requestFocus()
    }
    val editor = RoleColors.of(LineoRole.Editor)
    val underline = RoleColors.of(LineoRole.ErrorUnderline).container
    val span = (line.evaluation as? LineEvaluation.Failed)?.error?.span
    val style = MaterialTheme.typography.headlineMedium
        .asExpression()
        .copy(color = editor.content, textAlign = TextAlign.End)

    if (caret != null) {
        BasicTextField(
            value = TextFieldValue(
                text = line.text,
                selection = TextRange(caret.coerceIn(0, line.text.length)),
            ),
            // A notepad line is one expression, so a newline is never text: it is the split
            // the document already has a shape for. The return key is *refused* rather than
            // accepted and split afterwards — accepting it leaves the keyboard's composing
            // buffer holding `x\n` while the document holds two lines, and the next letter
            // typed arrives as `x\n3` and duplicates the line. Seen on a device.
            //
            // A paste is told apart by not being a single newline dropped into what was
            // already there, and goes to the text path, where `NotepadState.setText` turns
            // each newline into a line of its own.
            onValueChange = { value ->
                if (value.text.isReturnKeyOn(line.text)) {
                    onNewLine()
                } else {
                    onTextChange(value.text, value.selection.start)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = style,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = errorSpanTransformation(span, underline),
        )
    } else {
        // Spoken from the tree, never from the characters: `2^3` is "2 to the power of 3"
        // (`docs/CONVENTIONS.md` §8). Only a line that is *not* being edited is described
        // this way — in the field above, TalkBack has to read what is actually there,
        // character by character, or the caret and what is spoken stop agreeing.
        val words = rememberSpeechWords()
        val spoken = line.ast?.let { ast -> remember(ast, words) { ExpressionSpeech.of(ast, words) } }
        Text(
            text = line.text.underlining(span, underline),
            style = style,
            // One line here too, so a long line does not change height the moment it loses
            // focus. What is trimmed is the start: the end of an expression is where the user
            // was typing, and it is the half that lines up with the result underneath.
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = LineoDimens.MinTouchTarget)
                .clickable(onClick = onFocus)
                .then(
                    if (spoken == null) Modifier else Modifier.semantics { contentDescription = spoken },
                ),
        )
    }
}

/**
 * What the line came to, directly under it.
 *
 * [LineEvaluation.Blocked] is deliberately not painted as an error: the line is fine, it is
 * waiting on one that is not, and red would blame the user for the wrong line
 * (`docs/ARCHITECTURE.md` §3).
 */
@Composable
private fun Evaluation(
    line: NotepadLineUiState,
    ordinalOf: (LineId) -> Int?,
    onApplySuggestion: (CalcError.UnknownIdentifier) -> Unit,
) {
    when (val evaluation = line.evaluation) {
        LineEvaluation.Empty -> Unit

        is LineEvaluation.Value -> Text(
            // The display boundary of docs/CONVENTIONS.md §1: grouping and the locale
            // separator, resolved once by the shell from the locale and the settings. A line
            // that formatted numbers itself would answer an id-ID user in en-US.
            text = LocalQuantityFormat.current.format(evaluation.value).display(),
            style = MaterialTheme.typography.headlineSmall.asExpression(),
            color = RoleColors.of(LineoRole.Result).content,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )

        is LineEvaluation.Failed -> {
            Note(text = calcErrorMessage(evaluation.error), role = LineoRole.ErrorMessage)
            val unknown = (evaluation.error as? CalcError.UnknownIdentifier)?.takeIf { it.suggestion != null }
            if (unknown != null) {
                FixChip(label = requireNotNull(unknown.suggestion), onFix = { onApplySuggestion(unknown) })
            }
        }

        is LineEvaluation.Blocked -> {
            val ordinal = ordinalOf(evaluation.cause)
            Note(
                text = if (ordinal == null) {
                    stringResource(R.string.notepad_blocked_missing)
                } else {
                    stringResource(R.string.notepad_blocked, ordinal)
                },
                role = LineoRole.Result,
            )
        }
    }
}

/**
 * A sentence under a line: an error message, or what a blocked line is waiting for.
 *
 * The error one gets a container because it has to be readable as text — `onErrorContainer`
 * on `errorContainer`, the split §10 grew after the first mapping put a message at 1.31:1.
 * A blocked note is not an error and takes none, so the two cannot be confused at a glance.
 */
@Composable
private fun Note(text: String, role: LineoRole) {
    val colors = RoleColors.of(role)
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = colors.content,
        modifier = Modifier
            .padding(top = LineoDimens.Grid)
            .clip(RoundedCornerShape(NoteCornerRadius))
            .background(if (role == LineoRole.ErrorMessage) colors.container else Color.Transparent)
            .padding(horizontal = LineoDimens.Grid, vertical = LineoDimens.Grid),
    )
}

/** Tap to replace the name the engine did not recognise with the one it thinks was meant. */
@Composable
private fun FixChip(label: String, onFix: () -> Unit) {
    val chip = RoleColors.of(LineoRole.SuggestionChip)
    Text(
        text = stringResource(app.lineo.ui.R.string.editor_fix_to, label),
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
 * Marks [span] as the part that is wrong, in the field the caret is in.
 *
 * The offsets are unchanged, so the mapping is the identity one and the caret keeps landing
 * where the user put it. The span is coerced because it describes the text the engine last
 * saw, and the user may have typed since.
 */
private fun errorSpanTransformation(span: IntRange?, colour: Color) =
    VisualTransformation { original ->
        TransformedText(original.text.underlining(span, colour), OffsetMapping.Identity)
    }

/**
 * The same marking for a line that is not being typed in.
 *
 * Colour *and* rule, never one alone: the rule is invisible to anyone who cannot see a
 * two-pixel line, and colour by itself is what `docs/CONVENTIONS.md` §10 forbids.
 */
private fun String.underlining(span: IntRange?, colour: Color): AnnotatedString {
    if (span == null) return AnnotatedString(this)
    val start = span.first.coerceIn(0, length)
    val end = (span.last + 1).coerceIn(start, length)
    return buildAnnotatedString {
        append(this@underlining)
        addStyle(SpanStyle(color = colour, textDecoration = TextDecoration.Underline), start, end)
    }
}

/** Wide enough for three digits, more lines than a notepad document is expected to hold. */
private val GutterWidth = 28.dp
private val NoteCornerRadius = 8.dp
private val ChipCornerRadius = 16.dp

/**
 * Whether [this] is [previous] with exactly one newline dropped into it — the return key.
 *
 * Anything else carrying a newline is a paste, and a paste means as many lines as it has.
 */
private fun String.isReturnKeyOn(previous: String): Boolean =
    count { it == '\n' } == 1 && replaceFirst("\n", "") == previous
