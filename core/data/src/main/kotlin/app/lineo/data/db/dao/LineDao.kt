package app.lineo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import app.lineo.data.db.entity.LineEntity
import kotlinx.coroutines.flow.Flow

/**
 * Editing the contents of a document.
 *
 * `touchDocument` writes to `documents` rather than `lines`, which is deliberate: every
 * edit here changes the document's `updatedAt`, and keeping it in this DAO lets [reorder]
 * do both inside one transaction.
 */
@Dao
internal interface LineDao {

    @Query("SELECT * FROM lines WHERE documentId = :documentId ORDER BY ordinal ASC")
    fun linesStream(documentId: Long): Flow<List<LineEntity>>

    @Query("SELECT * FROM lines WHERE documentId = :documentId ORDER BY ordinal ASC")
    suspend fun lines(documentId: Long): List<LineEntity>

    @Insert
    suspend fun insertLine(line: LineEntity): Long

    @Upsert
    suspend fun upsertLines(lines: List<LineEntity>)

    @Query("DELETE FROM lines WHERE id = :id")
    suspend fun deleteLine(id: Long)

    @Query("UPDATE documents SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touchDocument(id: Long, updatedAt: Long)

    /**
     * Rewrites the whole ordinal range of a document in one transaction.
     *
     * Reordering row by row would let a reader observe two lines sharing an ordinal, and
     * would cost one write per line (`docs/ANDROID_STANDARDS.md` §4: bulk updates go in a
     * transaction).
     */
    @Transaction
    suspend fun reorder(documentId: Long, orderedLineIds: List<Long>, updatedAt: Long) {
        val existing = lines(documentId).associateBy { it.id }
        val renumbered = orderedLineIds.mapIndexedNotNull { index, id ->
            existing[id]?.copy(ordinal = index)
        }
        upsertLines(renumbered)
        touchDocument(documentId, updatedAt)
    }
}
