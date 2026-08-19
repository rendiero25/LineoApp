package app.lineo.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lineo.R
import app.lineo.data.model.HistoryEntry
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.LineoTypography
import app.lineo.ui.theme.RoleColors
import app.lineo.ui.theme.asExpression

/**
 * The tape: what has been calculated, newest first, and a way back into it.
 *
 * Stateless. Everything shown comes from [entries], and the two things a user can do leave
 * through [onReuse] and [onClear] — the screen neither reads the database nor writes it.
 *
 * Tapping an entry reuses the **expression**, not the result: the expression is the thing
 * that can be recalculated and edited, and the result is already on screen above it. What
 * that means for the notepad is a new line, which is why this screen closes as it does it.
 *
 * @param entries newest first, already capped by the repository.
 * @param onReuse the expression the user tapped.
 * @param onClear empties the tape.
 */
@Composable
internal fun HistoryScreen(
    entries: List<HistoryEntry>,
    onReuse: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RoleColors.of(LineoRole.Editor).container),
    ) {
        if (entries.isEmpty()) {
            EmptyTape(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(LineoDimens.EditorPadding),
                verticalArrangement = Arrangement.spacedBy(LineoDimens.LineGap),
            ) {
                items(entries, key = { it.id }) { entry ->
                    HistoryRow(entry = entry, onReuse = { onReuse(entry.expression) })
                }
            }
            ClearButton(onClear = onClear)
        }
    }
}

/**
 * One tape row: what was typed, and what it came to.
 *
 * The same alignment as a notepad line — expression above, result below, both read from the
 * trailing edge — so the tape looks like what it is a record of.
 */
@Composable
private fun HistoryRow(entry: HistoryEntry, onReuse: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RowCornerRadius))
            .clickable(onClick = onReuse)
            .padding(LineoDimens.Grid),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = entry.expression,
            style = LineoTypography.Result.asExpression(),
            color = RoleColors.of(LineoRole.Editor).content,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = entry.resultText,
            style = MaterialTheme.typography.titleMedium,
            color = RoleColors.of(LineoRole.Result).content,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * What an empty tape says.
 *
 * A sentence rather than a blank screen: the first thing a new user does is look for what
 * this screen is, and "nothing yet" answers that where emptiness does not.
 */
@Composable
private fun EmptyTape(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = RoleColors.of(LineoRole.Editor).content,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(LineoDimens.EditorPadding),
        )
    }
}

/** Empties the tape. Below the list, where it cannot be hit while reaching for an entry. */
@Composable
private fun ClearButton(onClear: () -> Unit) {
    val colors = RoleColors.of(LineoRole.Clear)
    Box(
        modifier = Modifier
            .padding(LineoDimens.EditorPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(RowCornerRadius))
            .background(colors.container)
            .clickable(onClick = onClear)
            .padding(LineoDimens.LineGap),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.history_clear),
            style = MaterialTheme.typography.titleMedium,
            color = colors.content,
        )
    }
}

private val RowCornerRadius = 12.dp
