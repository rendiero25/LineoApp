package app.lineo.data.repository

import app.lineo.data.db.dao.DocumentDao
import app.lineo.data.db.dao.LineDao
import app.lineo.data.db.entity.DocumentEntity
import app.lineo.data.db.entity.LineEntity
import app.lineo.data.model.CURRENT_DOCUMENT_SCHEMA
import app.lineo.data.model.Document
import app.lineo.data.model.Line
import app.lineo.engine.LineId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * [DocumentRepository] over Room.
 *
 * The [Clock] is injected rather than read from [Instant.now] so that ordering is
 * reproducible in tests; it is the only source of time in this class.
 */
internal class RoomDocumentRepository @Inject constructor(
    private val documents: DocumentDao,
    private val lines: LineDao,
    private val clock: Clock,
) : DocumentRepository {

    override fun getDocumentsStream(): Flow<List<Document>> =
        documents.documentsStream().map { entities -> entities.map { it.toModel() } }

    override fun getDocumentStream(id: Long): Flow<Document?> =
        documents.documentStream(id).map { it?.toModel() }

    override fun getLinesStream(documentId: Long): Flow<List<Line>> =
        lines.linesStream(documentId).map { entities -> entities.map { it.toModel() } }

    override suspend fun createDocument(title: String): Long {
        val now = clock.millis()
        return documents.insertDocument(
            DocumentEntity(
                title = title,
                schemaVersion = CURRENT_DOCUMENT_SCHEMA,
                createdAt = now,
                updatedAt = now,
                sortIndex = 0,
            ),
        )
    }

    override suspend fun renameDocument(id: Long, title: String) {
        val existing = documents.document(id) ?: return
        documents.upsertDocument(existing.copy(title = title, updatedAt = clock.millis()))
    }

    override suspend fun deleteDocument(id: Long) = documents.deleteDocument(id)

    override suspend fun appendLine(documentId: Long, source: String, label: String?): LineId {
        val ordinal = lines.lines(documentId).size
        val id = lines.insertLine(
            LineEntity(documentId = documentId, ordinal = ordinal, source = source, label = label),
        )
        lines.touchDocument(documentId, clock.millis())
        return LineId(id)
    }

    override suspend fun updateLine(line: Line) {
        lines.upsertLines(listOf(line.toEntity()))
        lines.touchDocument(line.documentId, clock.millis())
    }

    override suspend fun deleteLine(documentId: Long, id: LineId) {
        lines.deleteLine(id.value)
        lines.touchDocument(documentId, clock.millis())
    }

    override suspend fun reorderLines(documentId: Long, orderedLineIds: List<LineId>) =
        lines.reorder(documentId, orderedLineIds.map { it.value }, clock.millis())
}

private fun DocumentEntity.toModel() = Document(
    id = id,
    title = title,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    sortIndex = sortIndex,
    schemaVersion = schemaVersion,
)

private fun LineEntity.toModel() = Line(
    id = LineId(id),
    documentId = documentId,
    ordinal = ordinal,
    source = source,
    label = label,
)

private fun Line.toEntity() = LineEntity(
    id = id.value,
    documentId = documentId,
    ordinal = ordinal,
    source = source,
    label = label,
)
