package app.lineo.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.lineo.R
import app.lineo.ui.theme.LineoDimens

/**
 * The overflow affordance in the top corner: three dots stacked vertically.
 *
 * Drawn from three circles rather than pulled from an icon library. The library would be a
 * dependency, an APK entry, and a §7 decision, all to describe a shape that is three
 * circles — and `docs/CONVENTIONS.md` §10 asks for Material Symbols where an icon carries
 * meaning, which this one only borrows by convention.
 *
 * It draws the affordance and nothing else. What it opens is [ModuleMenu]'s business, which
 * is where the destinations are known — P0-11b-2 left this button opening nothing and
 * recorded that as a defect that must not ship, and P1-11 is the task that had to see it
 * closed. It is closed: history, the modules and settings are all one tap away.
 */
@Composable
internal fun OverflowMenuButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val description = stringResource(R.string.menu_more_options)
    Column(
        modifier = modifier
            .padding(LineoDimens.EditorPadding)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .size(LineoDimens.MinTouchTarget)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(DotGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(DOTS) {
            Column(
                modifier = Modifier
                    .size(DotSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant),
            ) {}
        }
    }
}

private const val DOTS = 3
private val DotSize = 4.dp
private val DotGap = 3.dp
