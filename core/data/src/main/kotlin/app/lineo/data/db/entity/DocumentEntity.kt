package app.lineo.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The `documents` table of `docs/ARCHITECTURE.md` §6.
 *
 * Entities are internal and never leave `:core:data`; repositories map them to the models
 * in `app.lineo.data.model`. That keeps the storage shape free to change without touching
 * a feature, which is the point of having a repository at all
 * (`docs/ANDROID_STANDARDS.md` §1).
 *
 * Timestamps are epoch milliseconds. No column in this database is floating point.
 */
@Entity(tableName = "documents")
internal data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val schemaVersion: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val sortIndex: Int,
)
