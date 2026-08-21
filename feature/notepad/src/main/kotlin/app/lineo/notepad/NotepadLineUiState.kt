package app.lineo.notepad

import app.lineo.engine.LineId
import app.lineo.engine.parser.Ast

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
    /**
     * How the parser read this line, or `null` while it does not parse.
     *
     * For the screen reader and nothing else (`docs/CONVENTIONS.md` §8): `2^3` is spoken as
     * "2 to the power of 3", and only the tree knows that the `^` was a power rather than
     * a character. It rides on the state because the evaluation already parsed the line.
     */
    val ast: Ast? = null,
)
