package app.lineo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.lineo.data.db.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun entriesStream(limit: Int): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entry: HistoryEntity): Long

    @Query("DELETE FROM history")
    suspend fun clear()

    /**
     * Drops everything past the newest [limit] rows.
     *
     * The free tier caps history at 50 (`TASKS.md` P1-06); the cap is applied on write so
     * the table cannot grow without bound between sessions.
     */
    @Query(
        "DELETE FROM history WHERE id NOT IN " +
            "(SELECT id FROM history ORDER BY createdAt DESC, id DESC LIMIT :limit)",
    )
    suspend fun trimTo(limit: Int)
}
