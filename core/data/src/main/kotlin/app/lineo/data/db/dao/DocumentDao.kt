package app.lineo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import app.lineo.data.db.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

/**
 * The document list. Line editing lives in [LineDao].
 *
 * Every read returns a [Flow] or is a `suspend` function, so nothing here can run on the
 * main thread by accident (`docs/ANDROID_STANDARDS.md` §4).
 */
@Dao
internal interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY sortIndex ASC, updatedAt DESC")
    fun documentsStream(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun documentStream(id: Long): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun document(id: Long): DocumentEntity?

    @Insert
    suspend fun insertDocument(document: DocumentEntity): Long

    @Upsert
    suspend fun upsertDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: Long)
}
