package app.lineo.ui.showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.RoleColors
import app.lineo.ui.theme.asExpression

/**
 * Every token in `docs/CONVENTIONS.md` §10 rendered at once.
 *
 * This is the reference screen the Paparazzi snapshots are taken from, so a change to the
 * palette, the type scale, or the role mapping shows up as a picture diff in review rather
 * than as a number nobody reads.
 *
 * The text is deliberately not translatable: it is either a numeric sample, a keypad
 * glyph, or the name of a colour token. Nothing here is user-facing copy, so
 * `AGENTS.md` §5's string-resource rule does not apply — and must not be worked around
 * anywhere that copy *is* user-facing.
 */
@Composable
fun TokenShowcase(modifier: Modifier = Modifier) {
    val editor = RoleColors.of(LineoRole.Editor)
    Surface(modifier = modifier.fillMaxWidth(), color = editor.container) {
        Column(
            modifier = Modifier.padding(LineoDimens.EditorPadding),
            verticalArrangement = Arrangement.spacedBy(LineoDimens.LineGap),
        ) {
            EditorSample()
            NotepadLineSample()
            ErrorSample()
            KeyRowSample()
            ChipRowSample()
        }
    }
}

/** The display pair of §10: the expression being typed, and its result beneath. */
@Composable
private fun EditorSample() {
    val editor = RoleColors.of(LineoRole.Editor)
    val result = RoleColors.of(LineoRole.Result)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "1 234,5 + 67,89",
            style = MaterialTheme.typography.displayMedium.asExpression(),
            color = editor.content,
        )
        Text(
            text = "1 302,39",
            style = MaterialTheme.typography.displaySmall.asExpression(),
            color = result.content,
        )
    }
}

/**
 * A notepad line: source on the start edge, result in its own column at the end.
 *
 * Each text is placed by a [Box] rather than by `textAlign`. The text itself is forced
 * left to right by [asExpression], so `TextAlign.Start` inside it would mean *left* even
 * in an RTL layout and the two columns would collide. Alignment is a layout decision and
 * has to stay with the layout.
 */
@Composable
private fun NotepadLineSample() {
    val editor = RoleColors.of(LineoRole.Editor)
    val result = RoleColors.of(LineoRole.Result)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "rent = 8 500 000",
                style = MaterialTheme.typography.bodyLarge.asExpression(),
                color = editor.content,
            )
        }
        Text(
            text = "8 500 000",
            style = MaterialTheme.typography.bodyLarge.asExpression(),
            color = result.content,
        )
    }
}

/**
 * The error pair: an underline at the offending span, and the message below it.
 *
 * §10 is explicit that error state is never colour alone. The underline marks the span,
 * the message carries the words; a screenshot with the colour stripped still reads.
 */
@Composable
private fun ErrorSample() {
    val editor = RoleColors.of(LineoRole.Editor)
    val error = RoleColors.of(LineoRole.Error)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "sni(1)",
            style = MaterialTheme.typography.bodyLarge.asExpression(),
            color = editor.content,
        )
        Box(
            modifier = Modifier
                .width(ErrorUnderlineSampleWidth)
                .height(ErrorUnderlineThickness)
                .background(error.container),
        )
        Text(
            text = "UnknownIdentifier @3..6",
            style = MaterialTheme.typography.bodySmall,
            color = error.content,
            modifier = Modifier
                .padding(top = LineoDimens.Grid)
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = LineoDimens.Grid),
        )
    }
}

/** One key per keypad role, in the order the §10 table lists them. */
@Composable
private fun KeyRowSample() {
    Row(horizontalArrangement = Arrangement.spacedBy(LineoDimens.KeyGap)) {
        KeySample(label = "7", role = LineoRole.Digit)
        KeySample(label = "×", role = LineoRole.Operator)
        KeySample(label = "=", role = LineoRole.Equals)
        KeySample(label = "AC", role = LineoRole.Clear)
        KeySample(label = "sin", role = LineoRole.Function)
    }
}

@Composable
private fun KeySample(label: String, role: LineoRole) {
    val colors = RoleColors.of(role)
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = LineoDimens.KeyMinSize, minHeight = LineoDimens.KeyMinSize)
            .clip(RoundedCornerShape(KeyCornerRadius))
            .background(colors.container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = colors.content,
        )
    }
}

/** The two chip-shaped roles: a suggestion, and a stale currency rate. */
@Composable
private fun ChipRowSample() {
    Row(horizontalArrangement = Arrangement.spacedBy(LineoDimens.KeyGap)) {
        ChipSample(label = "sin", role = LineoRole.SuggestionChip)
        ChipSample(label = "IDR 22 Aug", role = LineoRole.StaleRateBadge)
    }
}

@Composable
private fun ChipSample(label: String, role: LineoRole) {
    val colors = RoleColors.of(role)
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = LineoDimens.MinTouchTarget)
            .clip(RoundedCornerShape(ChipCornerRadius))
            .background(colors.container)
            .padding(horizontal = LineoDimens.EditorPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.content,
        )
    }
}

private val KeyCornerRadius = 20.dp
private val ChipCornerRadius = 16.dp
private val ErrorUnderlineThickness = 2.dp
private val ErrorUnderlineSampleWidth = 64.dp
