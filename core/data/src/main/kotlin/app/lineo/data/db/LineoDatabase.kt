package app.lineo.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import app.lineo.data.db.dao.DocumentDao
import app.lineo.data.db.dao.FormulaDao
import app.lineo.data.db.dao.HistoryDao
import app.lineo.data.db.dao.LineDao
import app.lineo.data.db.dao.RateDao
import app.lineo.data.db.entity.DocumentEntity
import app.lineo.data.db.entity.FormulaEntity
import app.lineo.data.db.entity.HistoryEntity
import app.lineo.data.db.entity.LineEntity
import app.lineo.data.db.entity.RateCacheEntity

/**
 * The application database (`docs/ARCHITECTURE.md` §6).
 *
 * [DATABASE_VERSION] is the *table* version and is what migrations are written against.
 * It is not the same thing as the per-row `schemaVersion` on documents and formulas, which
 * versions the content format. Both exist because they change for different reasons.
 *
 * Schemas are exported to `core/data/schemas` and committed. A version bump without a
 * committed schema means no migration can ever be tested, so `exportSchema` stays true.
 *
 * There is deliberately no `fallbackToDestructiveMigration`. A failed migration must enter
 * recovery ([DatabaseRecovery]), never quietly delete a user's documents.
 */
@Database(
    entities = [
        DocumentEntity::class,
        LineEntity::class,
        HistoryEntity::class,
        FormulaEntity::class,
        RateCacheEntity::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
)
internal abstract class LineoDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun lineDao(): LineDao
    abstract fun historyDao(): HistoryDao
    abstract fun formulaDao(): FormulaDao
    abstract fun rateDao(): RateDao
}

/** Bump on any table change, and commit the exported schema in the same commit. */
internal const val DATABASE_VERSION = 1

/** The on-disk name. Referenced by the backup rules, so it is not a private detail. */
internal const val DATABASE_NAME = "lineo.db"
