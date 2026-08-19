package app.lineo.notepad

import app.lineo.data.model.Line
import app.lineo.data.repository.DocumentRepository
import kotlinx.coroutines.flow.first

/**
 * Reads and writes a [NotepadDocument] as the `documents` / `lines` rows of
 * `docs/ARCHITECTURE.md` §6.
 *
 * The two models are nearly the same shape, and the one difference is the point of this
 * class: a row carries an `ordinal` column and a [NotepadLine] does not. A line does not know
 * where it sits — the document does, and it can only be right in one place — so the ordinal
 * is written from the position on the way out and thrown away on the way in.
 *
 * **The document is the authority on line ids, not the database.** A reference binds to
 * `lines.id` (§6), which means a line's id has to exist the moment the user presses Enter,
 * long before anything is written to disk; asking Room to allocate it would make every new
 * line a suspending call, and rewriting ids at save time would repoint references, which is
 * the one thing P1-01 exists to prevent. Rows are therefore written with the ids the document
 * already has, and [open] seeds the document's counter above every id in the table — see
 * [highestLineId].
 */
class NotepadStore(private val documents: DocumentRepository) {

    /**
     * Loads a document.
     *
     * Returns an empty document when the id is not in the store, which is also what a caller
     * gets for a document whose rows have all been deleted. Neither is an error: a notepad
     * with nothing in it is a notepad.
     */
    suspend fun open(documentId: Long): NotepadDocument {
        val rows = documents.getLinesStream(documentId).first()
        return NotepadDocument(
            lines = rows.map { NotepadLine(id = it.id, source = it.source, label = it.label) },
            nextLineId = highestLineId() + 1,
        )
    }

    /**
     * Opens the notepad, creating it the first time the app is run.
     *
     * Phase 1 has exactly one document — folders and a document list are P3-06 — so "the
     * notepad" is the oldest one in the store, and the id is returned alongside it because
     * every later save needs it.
     *
     * The document always comes back with at least one line. An empty list would leave the
     * screen with nothing to put the caret in, and the first thing any caller would do is
     * append one.
     */
    suspend fun openOrCreate(title: String): OpenNotepad {
        val existing = documents.getDocumentsStream().first().minByOrNull { it.id }
        val id = existing?.id ?: documents.createDocument(title)
        val document = open(id)
        return OpenNotepad(id = id, document = document.takeIf { it.lines.isNotEmpty() } ?: document.append())
    }

    /**
     * Writes [document] to [documentId], as it now stands.
     *
     * Only rows that actually changed are written, and rows the document no longer has are
     * deleted. A save on every keystroke is the design (see [NotepadAutosave]), so a save
     * that rewrote two hundred rows to record one character would put the whole document
     * through the disk on every press.
     */
    suspend fun save(documentId: Long, document: NotepadDocument) {
        val existing = documents.getLinesStream(documentId).first().associateBy { it.id }
        val kept = document.lines.map { it.id }.toSet()

        existing.keys.filterNot { it in kept }.forEach { documents.deleteLine(documentId, it) }

        document.lines.forEachIndexed { index, line ->
            val row = Line(
                id = line.id,
                documentId = documentId,
                ordinal = index,
                source = line.source,
                label = line.label,
            )
            if (existing[line.id] != row) documents.updateLine(row)
        }
    }

    /**
     * The largest line id anywhere in the store.
     *
     * Over every document, not only the one being opened. `lines.id` is one key space for the
     * whole table, so a counter seeded from one document would hand out an id another
     * document already owns, and the insert would collide with a row the user cannot see.
     *
     * It costs a read of every line in the store, once, when a document is opened — a notepad
     * holds a few hundred short lines, and the alternative is a `MAX(id)` query, which would
     * mean editing `:core:data` in a `:feature:notepad` task (rule 3). Worth revisiting when
     * P1-06 or P1-07 next opens that module for a reason of its own.
     */
    private suspend fun highestLineId(): Long = documents.getDocumentsStream().first()
        .flatMap { documents.getLinesStream(it.id).first() }
        .maxOfOrNull { it.id.value }
        ?: (NotepadDocument.FIRST_LINE_ID - 1)
}

/**
 * A document and the id to save it back under.
 *
 * The id is not on [NotepadDocument] because the document model has no idea it is stored —
 * `:feature:notepad` evaluates documents that were never on disk, and every test in P1-01
 * and P1-02 builds one.
 */
data class OpenNotepad(val id: Long, val document: NotepadDocument)
