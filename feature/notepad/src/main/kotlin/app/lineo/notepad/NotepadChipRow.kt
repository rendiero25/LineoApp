package app.lineo.notepad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.lineo.registry.EditorCommand
import app.lineo.ui.input.KeypadKey
import app.lineo.ui.input.keyLabel
import app.lineo.ui.layout.dockedBottomPadding
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.RoleColors
import app.lineo.ui.theme.asExpression

/**
 * The strip above the input surface — whichever surface that is.
 *
 * `docs/ARCHITECTURE.md` §5 says the chip row belongs above *either* surface, and until now
 * it was not: `%`, `^`, `√` and the argument separator lived on the accessory row, which
 * only appears with the system keyboard, so on a compact window keypad mode could not type
 * a percentage at all. That gap was recorded against P0-13 and this row is what closes it.
 *
 * Two kinds of chip, in a fixed order. The expression keys come first and never move, so a
 * thumb learns where `%` is; the suggestions follow, because they change with the caret and
 * anything after them would move under the finger. The row scrolls when it runs out of
 * width rather than shrinking its chips — a 48 dp target is a floor, not a starting point.
 *
 * @param keys the expression keys, from `:core:ui`. What they are is that module's decision,
 *   not this screen's.
 * @param suggestions names in scope at the caret, nearest first.
 * @param docked true when this row is the bottom-most thing in the window — that is, when
 *   the keypad is not below it. It then owns the inset that keeps it above the keyboard and
 *   the navigation bar; when the keypad is below, the keypad owns it and this row must not
 *   pad, or the two paddings would stack.
 */
@Composable
internal fun NotepadChipRow(
    keys: List<KeypadKey>,
    suggestions: List<NotepadSuggestion>,
    docked: Boolean,
    onCommand: (EditorCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RoleColors.of(LineoRole.Editor).container)
            .then(if (docked) Modifier.dockedBottomPadding() else Modifier)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = LineoDimens.KeypadEdge, vertical = LineoDimens.Grid),
        horizontalArrangement = Arrangement.spacedBy(LineoDimens.KeyGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        keys.forEach { key ->
            Chip(
                label = keyLabel(key),
                role = key.role,
                description = key.contentDescription?.let { stringResource(it) },
                onPress = { onCommand(key.command) },
            )
        }
        suggestions.forEach { suggestion ->
            Chip(
                label = suggestion.text,
                role = LineoRole.SuggestionChip,
                description = stringResource(
                    when (suggestion.kind) {
                        NotepadSuggestion.Kind.Variable -> R.string.notepad_suggestion_variable_description
                        NotepadSuggestion.Kind.Unit -> R.string.notepad_suggestion_unit_description
                    },
                    suggestion.text,
                ),
                onPress = { onCommand(EditorCommand.InsertText(suggestion.text)) },
            )
        }
    }
}

/**
 * One chip, sized to its label.
 *
 * Not to a share of the row: the row grows and shrinks as the caret moves between lines, and
 * a chip that changed width when a neighbour appeared would be a moving target.
 */
@Composable
private fun Chip(label: String, role: LineoRole, description: String?, onPress: () -> Unit) {
    val colors = RoleColors.of(role)
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = LineoDimens.MinTouchTarget, minHeight = LineoDimens.MinTouchTarget)
            .clip(RoundedCornerShape(ChipCornerRadius))
            .background(colors.container)
            .clickable(onClick = onPress)
            .padding(horizontal = LineoDimens.LineGap)
            .then(
                if (description == null) {
                    Modifier
                } else {
                    Modifier.semantics { contentDescription = description }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.asExpression(),
            color = colors.content,
        )
    }
}

private val ChipCornerRadius = 16.dp
