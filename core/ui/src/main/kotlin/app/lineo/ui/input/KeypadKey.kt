package app.lineo.ui.input

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import app.lineo.registry.EditorCommand
import app.lineo.ui.theme.LineoRole

/**
 * One key: what it shows, how it is coloured, and what it asks the editor to do.
 *
 * The command is data on the key rather than a branch in the click handler. A keypad that
 * decided per key what a press means would have to be edited every time a key is added,
 * and `docs/ARCHITECTURE.md` §5 wants that mapping in one place.
 *
 * @param label what the user sees, when it is the same in every language: a digit, an
 *   operator glyph, `sin⁻¹`. Empty when [labelRes] names the label instead.
 * @param labelRes the label as a string resource, for the few keys whose label is a *word*
 *   rather than notation — `ABC` and `123`, which name a script and a number system and
 *   change with the language (`AGENTS.md` §5). Resolved where the key is drawn, so the
 *   layout stays a pure function with no `Context` in reach of it.
 * @param iconRes optional icon for the key. If present, it takes precedence over the label.
 * @param role the token mapping row from `docs/CONVENTIONS.md` §10 this key belongs to.
 * @param command what pressing it emits.
 * @param contentDescription string resource TalkBack reads when [label] is not a word —
 *   `⌫` announces nothing useful. Null when the label already says it. A resource id and
 *   not a string, so the layout stays a pure function with no `Context` in reach of it.
 */
@Immutable
data class KeypadKey(
    val label: String = "",
    val role: LineoRole,
    val command: EditorCommand,
    @param:StringRes val contentDescription: Int? = null,
    @param:StringRes val labelRes: Int? = null,
    @param:DrawableRes val iconRes: Int? = null,
)
