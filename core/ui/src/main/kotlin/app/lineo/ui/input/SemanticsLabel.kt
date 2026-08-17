package app.lineo.ui.input

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Gives a key the label TalkBack should read, on the same node that handles the tap.
 *
 * `Modifier.semantics { }` on its own publishes an *unmerged* node. Next to a `clickable`,
 * which merges, the accessibility tree ends up with two nodes at identical bounds: one that
 * carries the description and cannot be activated, and one that can be activated and says
 * nothing. Verified in a `uiautomator` dump of the running keypad, which is the only place
 * that difference is visible — it looks identical on screen.
 *
 * `mergeDescendants = true` puts both on one node: a button that announces itself and can
 * be pressed. Returns the modifier unchanged for a key whose label already reads correctly,
 * since a description that repeats the label makes TalkBack say it twice.
 */
internal fun Modifier.semanticsLabel(description: String?): Modifier =
    if (description == null) {
        this
    } else {
        semantics(mergeDescendants = true) { contentDescription = description }
    }
