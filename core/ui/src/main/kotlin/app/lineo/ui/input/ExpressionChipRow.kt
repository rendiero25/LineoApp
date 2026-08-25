package app.lineo.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lineo.registry.EditorCommand
import app.lineo.ui.layout.dockedBottomPadding
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.RoleColors
import app.lineo.ui.theme.asExpression

/**
 * The strip of expression keys that sits above an input surface — whichever surface that is.
 *
 * `docs/ARCHITECTURE.md` §5 puts this row above *either* surface, and the reason is the gap
 * P0-13 recorded: `(`, `)`, `^`, `√`, `%` and the argument separator are not on the keypad,
 * so a screen that shows this row only with the system keyboard cannot type a bracket in
 * keypad mode at all.
 *
 * One implementation, because there were two. `:feature:notepad` had its own copy to get the
 * row above both of its surfaces, and a feature may not read another feature's code
 * (`AGENTS.md` §3) — so the second surface to need it, the scientific screen, could not have
 * it. The copy and this differed in nothing a user could see: same chip, same corner, same
 * 48 dp floor.
 *
 * @param keys the expression keys. What they are is this module's decision — see
 *   [accessoryKeys] — and not a screen's.
 * @param onCommand where a press goes. The row holds no state: it is a view of [keys] and
 *   [trailing], and the editor is what a command reaches.
 * @param trailing chips after the keys, contributed by the screen — in-scope variable names,
 *   recent units. They come last and the keys never move, so a thumb learns where `%` is;
 *   anything that changed with the caret in front of them would shift the row under the
 *   finger.
 * @param docked true when this row is the bottom-most thing in the window, and so owns the
 *   inset that keeps it above the keyboard and the navigation bar. False when a keypad sits
 *   below it and owns that inset instead — pad in both places and the two would stack.
 */
@Composable
fun ExpressionChipRow(
    keys: List<KeypadKey>,
    onCommand: (EditorCommand) -> Unit,
    modifier: Modifier = Modifier,
    trailing: List<ExpressionChip> = emptyList(),
    docked: Boolean = true,
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
        trailing.forEach { chip ->
            Chip(
                label = chip.label,
                role = chip.role,
                description = chip.contentDescription,
                onPress = { onCommand(chip.command) },
            )
        }
    }
}

/**
 * A grid of expression keys, used in landscape where vertical space allows more structure.
 *
 * Arranges [keys] and [trailing] into [columns]. Unlike the row variant, this does not
 * scroll — it is meant for a side pane where the width is fixed and the goal is to make the
 * buttons large and stable targets.
 */
@Composable
fun ExpressionChipGrid(
    keys: List<KeypadKey>,
    onCommand: (EditorCommand) -> Unit,
    modifier: Modifier = Modifier,
    trailing: List<ExpressionChip> = emptyList(),
    columns: Int = 3,
) {
    val allChips = keys.map { key ->
        ExpressionChip(
            label = keyLabel(key),
            role = key.role,
            command = key.command,
            contentDescription = key.contentDescription?.let { stringResource(it) },
        )
    } + trailing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RoleColors.of(LineoRole.Editor).container)
            .padding(horizontal = LineoDimens.KeypadEdge, vertical = LineoDimens.Grid),
        verticalArrangement = Arrangement.spacedBy(LineoDimens.KeyGap),
    ) {
        allChips.chunked(columns).forEach { rowChips ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LineoDimens.KeyGap),
            ) {
                rowChips.forEach { chip ->
                    Chip(
                        label = chip.label,
                        role = chip.role,
                        description = chip.contentDescription,
                        onPress = { onCommand(chip.command) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Fill the rest of the row with empty boxes to keep the grid aligned.
                repeat(columns - rowChips.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * A chip a screen contributes to [ExpressionChipRow], beyond the expression keys.
 *
 * Resolved strings rather than resource ids: what a suggestion is called and how it is
 * described belong to the screen that knows why it is being suggested, and `:core:ui` has no
 * business owning a string about a variable in scope.
 *
 * @param label what is drawn. Also what is typed, for a suggestion.
 * @param role the colour role — [LineoRole.SuggestionChip] for anything a screen contributes,
 *   unless it has a reason.
 * @param command what pressing it emits.
 * @param contentDescription what a screen reader says instead of the bare label. Null only
 *   when the label already reads correctly on its own (`docs/CONVENTIONS.md` §8).
 */
@Immutable
data class ExpressionChip(
    val label: String,
    val role: LineoRole,
    val command: EditorCommand,
    val contentDescription: String? = null,
)

/**
 * One chip, sized to its label.
 *
 * Not to a share of the row: the row grows and shrinks as the caret moves between lines, and
 * a chip that changed width when a neighbour appeared would be a moving target. The row
 * scrolls when it runs out of width rather than shrinking its chips — 48 dp is a floor, not
 * a starting point.
 */
@Composable
private fun Chip(
    label: String,
    role: LineoRole,
    description: String?,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RoleColors.of(role)
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = LineoDimens.MinTouchTarget, minHeight = LineoDimens.MinTouchTarget)
            .clip(RoundedCornerShape(ChipCornerRadius))
            .background(colors.container)
            .clickable(onClick = onPress)
            .padding(horizontal = LineoDimens.LineGap)
            .semanticsLabel(description),
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
