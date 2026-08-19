package app.lineo.notepad

import app.lineo.data.model.Document
import app.lineo.data.model.Line
import app.lineo.data.repository.DocumentRepository
import app.lineo.engine.LineId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * The store's other half, in memory.
 *
 * A fake rather than Room, which is what the interface in `:core:data` exists for: this test
 * is about the mapping between a [NotepadDocument] and rows, and running it on Robolectric
 * would measure Room instead. The one thing it does model faithfully is that `lines.id` is a
 * single key space for the whole table — the constraint the id watermark exists for — so a
 * duplicate id across two documents fails here as it would fail in SQLite.
 */
internal class FakeDocumentRepository : DocumentRepository {

    private val documents = MutableStateFlow(emptyList<Document>())
    private val lines = MutableStateFlow(emptyList<Line>())

    /** Every write of a row, in order. What the "only changed rows" assertions count. */
    val writes = mutableListOf<Line>()

    override fun getDocumentsStream(): Flow<List<Document>> = documents

    override fun getDocumentStream(id: Long): Flow<Document?> =
        documents.map { all -> all.firstOrNull { it.id == id } }

    override fun getLinesStream(documentId: Long): Flow<List<Line>> =
        lines.map { all -> all.filter { it.documentId == documentId }.sortedBy { it.ordinal } }

    override suspend fun createDocument(title: String): Long {
        val id = (documents.value.maxOfOrNull { it.id } ?: 0L) + 1
        documents.value += Document(
            id = id,
            title = title,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
            sortIndex = 0,
        )
        return id
    }

    override suspend fun renameDocument(id: Long, title: String) {
        documents.value = documents.value.map { if (it.id == id) it.copy(title = title) else it }
    }

    override suspend fun deleteDocument(id: Long) {
        documents.value = documents.value.filterNot { it.id == id }
        lines.value = lines.value.filterNot { it.documentId == id }
    }

    override suspend fun appendLine(documentId: Long, source: String, label: String?): LineId {
        val id = LineId((lines.value.maxOfOrNull { it.id.value } ?: 0L) + 1)
        val ordinal = lines.value.count { it.documentId == documentId }
        val line = Line(id, documentId, ordinal, source, label)
        lines.value += line
        writes += line
        return id
    }

    override suspend fun updateLine(line: Line) {
        val clash = lines.value.firstOrNull { it.id == line.id && it.documentId != line.documentId }
        check(clash == null) { "line id ${line.id.value} already belongs to document ${clash?.documentId}" }
        lines.value = lines.value.filterNot { it.id == line.id } + line
        writes += line
    }

    override suspend fun deleteLine(documentId: Long, id: LineId) {
        lines.value = lines.value.filterNot { it.id == id }
    }

    override suspend fun reorderLines(documentId: Long, orderedLineIds: List<LineId>) {
        val byId = lines.value.associateBy { it.id }
        val renumbered = orderedLineIds.mapIndexedNotNull { index, id -> byId[id]?.copy(ordinal = index) }
        lines.value = lines.value.filterNot { it.documentId == documentId } + renumbered
    }
}
