package app.lineo.notepad

import app.lineo.engine.LineId

/**
 * One line of a notepad document: its identity and the text behind it.
 *
 * [source] is stored text — references in it name line ids, never ordinals
 * (`docs/GRAMMAR.md` §3.7). Use [NotepadDocument.displayTextOf] to get the form the user
 * should see and [NotepadDocument.setDisplayText] to write the form they typed.
 *
 * There is no ordinal here on purpose. A line does not know where it sits; the document
 * does, and it can only be right in one place. There is no result here either: results are
 * recomputed, never stored, because a stored one goes stale as soon as a line it depends on
 * changes (`docs/ARCHITECTURE.md` §6).
 *
 * @param label the name this line defines, when it defines one. Carried rather than derived
 *   so that the storage column of §6 round-trips; keeping it in step with [source] is the
 *   evaluator's job in P1-02, which parses the line anyway.
 */
data class NotepadLine(
    val id: LineId,
    val source: String,
    val label: String? = null,
)
