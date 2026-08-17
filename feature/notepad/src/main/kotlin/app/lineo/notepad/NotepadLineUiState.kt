package app.lineo.notepad

import app.lineo.engine.LineId

/**
 * One line as the screen should show it.
 *
 * [text] is display text: references in it name ordinals, because that is what the user
 * typed and what they can read. What is stored names ids (`docs/GRAMMAR.md` §3.7), and
 * nothing above this class ever sees that form.
 *
 * @param ordinal the line's position, counting from 1. For the gutter, and for nothing else.
 */
data class NotepadLineUiState(
    val id: LineId,
    val ordinal: Int,
    val text: String,
    val evaluation: LineEvaluation,
)
