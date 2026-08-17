package app.lineo.ui.editor

import app.lineo.engine.CalcError
import app.lineo.engine.Quantity

/**
 * What the editor currently knows about the line being typed.
 *
 * Four states rather than the engine's two, because a line that has not been finished is
 * not a line that is wrong. `5 +` fails to parse, but the user is mid-keystroke and has
 * made no mistake yet; showing them a red underline for it would train them to ignore red
 * underlines. [Unfinished] is that case, and it renders as nothing at all.
 */
sealed interface EditorEvaluation {

    /** Nothing typed, or nothing but whitespace. */
    data object Empty : EditorEvaluation

    /**
     * The line does not parse *yet*: it ends mid-expression, or it has a bracket still
     * open. Distinguished from [Failure] so the editor can stay quiet.
     */
    data object Unfinished : EditorEvaluation

    /** The line evaluated. */
    data class Result(val value: Quantity) : EditorEvaluation

    /**
     * The line is wrong in a way that finishing it will not fix — an unknown name, a unit
     * mismatch, a domain error. This is what gets an underline and a message.
     */
    data class Failure(val error: CalcError) : EditorEvaluation
}
