package app.lineo.notepad

import app.lineo.engine.LineId

/**
 * The whole notepad screen, as state.
 *
 * One object, per `docs/ANDROID_STANDARDS.md` §1: no events fired at the UI, no second
 * channel for errors. A line that failed carries its failure in [lines]; a surface swap is
 * [textInputActive] changing.
 *
 * @param focused the line the caret is in, or `null` when nothing has focus.
 * @param caret where the caret sits in the focused line's [NotepadLineUiState.text].
 * @param textInputActive true when the system keyboard is the input surface, false when the
 *   keypad is. Set by the user pressing `ABC`, and by the context switch of
 *   `docs/ARCHITECTURE.md` §5 when focus moves to a line that begins with a letter.
 */
data class NotepadUiState(
    val lines: List<NotepadLineUiState> = emptyList(),
    val focused: LineId? = null,
    val caret: Int = 0,
    val textInputActive: Boolean = false,
) {

    /** The focused line, or `null`. */
    val focusedLine: NotepadLineUiState? get() = lines.firstOrNull { it.id == focused }
}
