package app.lineo.data.model

import app.lineo.engine.LineId

/**
 * One line of a document: the text the user typed, and nothing derived from it.
 *
 * Results are never stored. They are recomputed from [source], because a stored result
 * would silently go stale the moment a line it depends on changes.
 *
 * [id] is stable for the lifetime of the line and is what a reference in another line
 * binds to. [ordinal] exists only for display order (`docs/ARCHITECTURE.md` §6): inserting
 * a line above this one changes the ordinal and must not change the id.
 */
data class Line(
    val id: LineId,
    val documentId: Long,
    val ordinal: Int,
    val source: String,
    val label: String? = null,
)
