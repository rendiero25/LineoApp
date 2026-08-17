package app.lineo.notepad

import app.lineo.engine.CalcError
import app.lineo.engine.LineId
import app.lineo.engine.Quantity

/**
 * What the document evaluator knows about one line.
 *
 * [Blocked] is the state `docs/ARCHITECTURE.md` §3 asks for by name: an error on line 3 must
 * mark line 7 as *depends on line 3*, not as wrong. Line 7 is fine; it is waiting on
 * something that is not. Rendering it red would blame the user for the wrong line, and in a
 * long document that is most of the lines.
 *
 * There is no `Unfinished` here, unlike `EditorEvaluation` in `:core:ui`. That state belongs
 * to the line being typed, which the editor owns and the document does not: a line the user
 * has left behind half-written is simply wrong, and saying so is the honest thing.
 */
sealed interface LineEvaluation {

    /** Blank, or nothing but whitespace. */
    data object Empty : LineEvaluation

    /** The line evaluated. */
    data class Value(val value: Quantity) : LineEvaluation

    /** The line itself is wrong. This is what gets an underline. */
    data class Failed(val error: CalcError) : LineEvaluation

    /**
     * The line reads another line that has no value — one that failed, is blank, or is
     * blocked in its turn. [cause] is the line to point at.
     */
    data class Blocked(val cause: LineId) : LineEvaluation
}
