package app.lineo.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The `history` table. `createdAt` is indexed because the tape is always read newest
 * first and trimmed oldest first.
 */
@Entity(tableName = "history", indices = [Index("createdAt")])
internal data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val resultText: String,
    val createdAt: Long,
    val moduleId: String? = null,
)
