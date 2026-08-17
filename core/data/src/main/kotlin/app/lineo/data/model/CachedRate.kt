package app.lineo.data.model

import java.math.BigDecimal
import java.time.Instant

/**
 * One exchange rate as it was last fetched (`TASKS.md` P2-07).
 *
 * [rate] is a [BigDecimal] and is stored as text, never as a floating-point column:
 * money is the one thing this app cannot round wrong (`AGENTS.md` §2).
 *
 * [fetchedAt] is what the stale-rate badge shows. A rate is never silently discarded for
 * being old — offline serves the cached value with its date visible.
 */
data class CachedRate(
    val base: String,
    val quote: String,
    val rate: BigDecimal,
    val fetchedAt: Instant,
    val source: String,
)
