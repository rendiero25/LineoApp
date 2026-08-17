package app.lineo.data.repository

import app.lineo.data.model.Document
import app.lineo.data.model.Line
import app.lineo.engine.LineId
import kotlinx.coroutines.flow.Flow

/**
 * Notepad documents and their lines.
 *
 * The interface exists even though one implementation wraps one DAO
 * (`docs/ANDROID_STANDARDS.md` §1): it is what lets a feature be tested against a fake and
 * keeps Room out of every layer above this one.
 *
 * Every method that returns a stream is named `...Stream`; every write suspends. Nothing
 * here can be called from the main thread without the caller noticing.
 */
interface DocumentRepository {

    fun getDocumentsStream(): Flow<List<Document>>

    fun getDocumentStream(id: Long): Flow<Document?>

    fun getLinesStream(documentId: Long): Flow<List<Line>>

    /** Creates an empty document and returns its id. */
    suspend fun createDocument(title: String): Long

    suspend fun renameDocument(id: Long, title: String)

    suspend fun deleteDocument(id: Long)

    /** Appends a line to the end of [documentId] and returns its stable id. */
    suspend fun appendLine(documentId: Long, source: String, label: String? = null): LineId

    /** Rewrites the text of an existing line. The id, and every reference to it, survive. */
    suspend fun updateLine(line: Line)

    suspend fun deleteLine(documentId: Long, id: LineId)

    /**
     * Reorders a document to exactly [orderedLineIds].
     *
     * Only `ordinal` changes. A reference written as `line3` binds to a [LineId], so moving
     * a line must not repoint anything (`docs/ARCHITECTURE.md` §6).
     */
    suspend fun reorderLines(documentId: Long, orderedLineIds: List<LineId>)
}
