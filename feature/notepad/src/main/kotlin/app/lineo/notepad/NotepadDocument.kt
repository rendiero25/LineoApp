package app.lineo.notepad

import app.lineo.engine.LineId

/**
 * A notepad document: an ordered list of lines with identities that outlive their positions.
 *
 * The whole model exists to keep one promise, the one `docs/ARCHITECTURE.md` §6 makes:
 * **a reference binds to an id and nothing else.** Inserting, deleting and reordering move
 * lines around; none of them repoints a reference, because none of them touches an id.
 * `ordinal` is display, and it is derived from the position rather than stored, so it cannot
 * disagree with the list it describes.
 *
 * Immutable: every edit returns a new document. A notepad is small — a few hundred short
 * lines — and the alternative is a mutable graph that the incremental evaluator of P1-02
 * would have to defend itself against on every keystroke.
 *
 * @param lines in display order. The first line has ordinal 1.
 * @param nextLineId the id the next inserted line will take. Carried rather than computed
 *   from [lines] so that a deleted line's id is never handed out again — see [delete].
 */
data class NotepadDocument(
    val lines: List<NotepadLine> = emptyList(),
    val nextLineId: Long = FIRST_LINE_ID,
) {

    init {
        require(lines.all { it.id.value < nextLineId }) {
            "nextLineId $nextLineId would collide with an existing line"
        }
        require(lines.distinctBy { it.id }.size == lines.size) { "duplicate line id" }
    }

    /** The line with this id, or `null` when the document has none. */
    fun line(id: LineId): NotepadLine? = lines.firstOrNull { it.id == id }

    /** Where this line is displayed, counting from 1, or `null` when it is not in the document. */
    fun ordinalOf(id: LineId): Int? = lines.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.plus(1)

    /** Which line is displayed at this ordinal, or `null` when the document is shorter. */
    fun idAtOrdinal(ordinal: Int): LineId? = lines.getOrNull(ordinal - 1)?.id

    /**
     * Inserts a line and returns the document that results.
     *
     * [source] is stored text. To insert what a user typed, insert an empty line and then
     * use [setDisplayText] — the ordinals in typed text can only be resolved against the
     * document the line is already part of.
     */
    fun insertAt(index: Int, source: String = "", label: String? = null): NotepadDocument {
        require(index in 0..lines.size) { "index $index outside 0..${lines.size}" }
        val line = NotepadLine(id = LineId(nextLineId), source = source, label = label)
        return copy(
            lines = lines.subList(0, index) + line + lines.subList(index, lines.size),
            nextLineId = nextLineId + 1,
        )
    }

    /** Appends a line at the end. */
    fun append(source: String = "", label: String? = null): NotepadDocument =
        insertAt(lines.size, source, label)

    /** Replaces a line's stored text. Unknown ids are ignored — the line may have just been deleted. */
    fun edit(id: LineId, source: String, label: String? = null): NotepadDocument =
        copy(lines = lines.map { if (it.id == id) it.copy(source = source, label = label) else it })

    /**
     * Removes a line.
     *
     * References to it are left as they are, pointing at an id that is no longer there. They
     * are not rewritten to another line and they are not deleted from the text: the user
     * wrote them, and the only honest thing to show is a reference that is visibly broken —
     * [LineReferences.MISSING_TARGET] on screen, `UnknownIdentifier` from the engine, and
     * [danglingReferences] for the editor to mark. [nextLineId] does not go back, so the id
     * cannot be handed to a new line and quietly make those references resolve again to
     * something the user never meant.
     */
    fun delete(id: LineId): NotepadDocument = copy(lines = lines.filterNot { it.id == id })

    /** Moves the line at [fromIndex] so that it ends up at [toIndex]. */
    fun move(fromIndex: Int, toIndex: Int): NotepadDocument {
        require(fromIndex in lines.indices) { "fromIndex $fromIndex outside ${lines.indices}" }
        require(toIndex in lines.indices) { "toIndex $toIndex outside ${lines.indices}" }
        if (fromIndex == toIndex) return this
        val moved = lines.toMutableList()
        moved.add(toIndex, moved.removeAt(fromIndex))
        return copy(lines = moved)
    }

    /**
     * What the user should see for this line: stored text with every id shown as the ordinal
     * it currently sits at.
     */
    fun displayTextOf(id: LineId): String? =
        line(id)?.let { LineReferences.toDisplay(it.source, ::ordinalOf) }

    /**
     * Stores what the user typed, binding each ordinal they wrote to the line that occupies
     * it now.
     *
     * This is the moment a reference is fixed. Afterwards the document can be reordered
     * freely and the reference still means the line the user was looking at when they typed
     * it, which is the entire point of the id.
     */
    fun setDisplayText(id: LineId, typed: String, label: String? = null): NotepadDocument =
        edit(id, LineReferences.toCanonical(typed, ::idAtOrdinal), label)

    /** The lines this one refers to, in the order they appear in its text. */
    fun referencesOf(id: LineId): List<LineId> =
        line(id)?.let { LineReferences.referencedIds(it.source) }.orEmpty()

    /**
     * The lines that refer to this one — what a change to it will make stale, and what a
     * deletion of it will break.
     *
     * A flat lookup, not a graph: the transitive closure, the topological order and the
     * cycle detection all belong to P1-02, which owns incremental evaluation.
     */
    fun dependentsOf(id: LineId): List<LineId> =
        lines.filter { id in LineReferences.referencedIds(it.source) }.map { it.id }

    /** Every reference in the document that points at a line that is not in it. */
    fun danglingReferences(): Map<LineId, List<LineId>> =
        lines.associate { it.id to LineReferences.referencedIds(it.source).filter { ref -> line(ref) == null } }
            .filterValues { it.isNotEmpty() }

    companion object {

        /** Ids count from 1, so that zero can mean "no line" (`LineReferences.MISSING_TARGET`). */
        const val FIRST_LINE_ID: Long = 1L

        /**
         * A document holding these lines of stored text, ids assigned in order.
         *
         * For tests and for loading a document whose ids are already known to be sequential;
         * anything else should build the document up with [insertAt].
         */
        fun of(vararg sources: String): NotepadDocument =
            sources.fold(NotepadDocument()) { document, source -> document.append(source) }
    }
}
