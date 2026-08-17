package app.lineo.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The `formulas` table, written to from Phase 3 onwards but created now: adding a table to
 * a shipped database costs a migration, adding one before release costs nothing.
 *
 * `name` is unique — a formula is called by name from any line, so two formulas with the
 * same name would make an expression ambiguous.
 *
 * `params` holds the parameter names joined by a unit separator (`U+001F`), which no
 * identifier can contain.
 */
@Entity(tableName = "formulas", indices = [Index(value = ["name"], unique = true)])
internal data class FormulaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val source: String,
    val params: String,
    val schemaVersion: Int,
)
