package app.lineo.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lineo.R

/**
 * The overflow affordance in the top corner: three dots stacked vertically.
 *
 * Drawn from three circles rather than pulled from an icon library. The library would be a
 * dependency, an APK entry, and a §7 decision, all to describe a shape that is three
 * circles — and `docs/CONVENTIONS.md` §10 asks for Material Symbols where an icon carries
 * meaning, which this one only borrows by convention.
 *
 * On the same disc as the surface switch opposite it ([TopBarButton]), and in the same
 * colour: the two are the window's controls, and a pair reads as a pair only if both halves
 * are drawn the same way.
 */
@Composable
internal fun OverflowMenuButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val description = stringResource(R.string.menu_more_options)
    val dots = topBarButtonContent()
    TopBarButton(description = description, onClick = onClick, modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(DotGap, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            repeat(DOTS) {
                Column(
                    modifier = Modifier
                        .size(DotSize)
                        .clip(CircleShape)
                        .background(dots),
                ) {}
            }
        }
    }
}

private const val DOTS = 3
private val DotSize = 4.dp
private val DotGap = 3.dp
