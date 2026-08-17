package app.lineo.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing and target sizes, from `docs/CONVENTIONS.md` §10 and
 * `docs/ANDROID_STANDARDS.md` §2.
 *
 * Everything sits on a 4 dp grid. These are constants rather than a theme-local
 * composition value because none of them varies by scheme or window size — layout that
 * does vary branches on window size class instead, never on a dimension read from here.
 */
object LineoDimens {

    /** The grid every other value is a multiple of. */
    val Grid: Dp = 4.dp

    /** Padding along the leading and trailing edges of the keypad, so keys never touch the window. */
    val KeypadEdge: Dp = 16.dp

    /** Default gap between keypad keys. */
    val KeyGap: Dp = 6.dp

    /** Platform minimum touch target. Nothing interactive may be smaller. */
    val MinTouchTarget: Dp = 48.dp

    /**
     * Minimum height of a keypad key in compact width. Deliberately above
     * [MinTouchTarget]: the keypad is the most-tapped surface in the app, so it gets
     * more than the floor.
     */
    val KeyMinSize: Dp = 56.dp

    /** Padding between the editor content and the edge of its pane. */
    val EditorPadding: Dp = 16.dp

    /** Gap between a notepad line and the next. */
    val LineGap: Dp = 12.dp
}
