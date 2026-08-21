package app.lineo.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.RoleColors

/**
 * One option in a row of them: a category, a separator, a unit system.
 *
 * **Selection is never colour alone** (`docs/CONVENTIONS.md` §8). The selected chip takes
 * the accent container *and* a bolder label, so the choice survives a colour-blind reader,
 * a greyscale screenshot and Android's high-contrast themes. Out loud it is a
 * [Role.RadioButton]: `selectable` publishes the selected state, so TalkBack says "selected"
 * rather than leaving the user to infer it from a colour they cannot hear.
 *
 * Shared rather than redrawn per screen, because three screens were drawing the same box
 * with the same rounding and only one of them could be fixed at a time.
 *
 * @param description what TalkBack reads instead of [label], for a label that is a glyph or
 *   a bare number — "1,5" as a separator sample says nothing on its own.
 */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val colors = RoleColors.of(if (selected) LineoRole.Equals else LineoRole.SuggestionChip)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(ChipCornerRadius))
            .background(colors.container)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .defaultMinSize(minWidth = LineoDimens.MinTouchTarget, minHeight = LineoDimens.MinTouchTarget)
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
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = colors.content,
        )
    }
}

/** Material's medium shape, which `docs/CONVENTIONS.md` §10 gives to anything chip-sized. */
private val ChipCornerRadius = 12.dp
