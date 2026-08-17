package app.lineo.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The `lines` table. `id` is stable and is what a cross-line reference binds to; `ordinal`
 * is display order only (`docs/ARCHITECTURE.md` §6).
 *
 * `documentId` is indexed because every read is scoped to one document, and a table scan
 * per keystroke is exactly the synchronous work `docs/ANDROID_STANDARDS.md` §4 forbids.
 * Deleting a document cascades: an orphan line is unreachable but would still be carried
 * by every future query.
 */
@Entity(
    tableName = "lines",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("documentId"), Index("documentId", "ordinal")],
)
internal data class LineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val ordinal: Int,
    val source: String,
    val label: String? = null,
)
