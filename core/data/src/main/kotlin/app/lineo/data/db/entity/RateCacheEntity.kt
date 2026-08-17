package app.lineo.data.db.entity

import androidx.room.Entity

/**
 * The `rate_cache` table, keyed by the currency pair: one row per pair, replaced by each
 * fetch.
 *
 * `rate` is text holding a `BigDecimal`. A `REAL` column would reintroduce binary floating
 * point on the one value the app must never round wrong (`AGENTS.md` §2).
 */
@Entity(tableName = "rate_cache", primaryKeys = ["base", "quote"])
internal data class RateCacheEntity(
    val base: String,
    val quote: String,
    val rate: String,
    val fetchedAt: Long,
    val source: String,
)
