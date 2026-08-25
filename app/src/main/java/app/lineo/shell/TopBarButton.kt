package app.lineo.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lineo.ui.theme.LineoDimens

/**
 * A control in the top bar: a filled disc with something drawn in the middle of it.
 *
 * The two buttons the bar has — the surface switch and the overflow — are the same shape and
 * the same colour, because they are the same kind of thing: the window's controls rather than
 * the document's. One composable, so a change to that shape cannot reach one of them and miss
 * the other, which is what happened when the switch was a disc and the overflow was three
 * bare dots.
 *
 * The band is 60 dp: a 48 dp target with `KeyGap` above and below it, the same margin the
 * keypad leaves above its first row of keys, so the document sits between two equal ones.
 *
 * @param description what a screen reader says. The content is drawn with no description of
 *   its own — the whole button is one node.
 */
@Composable
internal fun TopBarButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .padding(horizontal = LineoDimens.EditorPadding, vertical = LineoDimens.KeyGap)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            // The *target* is 48 dp and the disc is smaller. `docs/CONVENTIONS.md` §8 is about
            // what a thumb has to hit, not about what is painted, so the drawn circle can be
            // trimmed while the thing you press stays the platform minimum.
            .size(LineoDimens.MinTouchTarget)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(TopBarDiscSize)
                .clip(CircleShape)
                .background(topBarButtonContainer()),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * The disc's colour: `onSurface`, the ink the keypad's digits are drawn in.
 *
 * Black, in other words, and the *same* black as `7` — one value, so the bar and the keypad
 * cannot drift apart, and the dark scheme flips it the way it flips every digit rather than
 * leaving a black hole on a black bar. A `RoleColors` role was tried first and read as a pale
 * chip on a pale bar: the roles describe keys, and a key is one of many while these two are
 * the only controls up here.
 */
@Composable
internal fun topBarButtonContainer(): Color = MaterialTheme.colorScheme.onSurface

/** What is drawn inside the disc: the surface the digits sit on, so the pairing is the same one. */
@Composable
internal fun topBarButtonContent(): Color = MaterialTheme.colorScheme.surface

/**
 * How big the drawn circle is, inside a 48 dp target.
 *
 * Smaller than the target on purpose: the bar is above the document and a full-width disc at
 * each end weighed more than two controls should. What shrinks is the paint, never the thing
 * a thumb has to hit.
 */
internal val TopBarDiscSize: Dp = 40.dp

/**
 * How big a glyph inside the disc is.
 *
 * Smaller than the 24 dp Material draws an icon at: inside a filled disc the glyph reads as
 * the *shape* of the button, and at 24 dp it filled it. 18 dp leaves the ring of colour that
 * makes the two buttons read as a pair.
 */
internal val TopBarIconSize: Dp = 18.dp
