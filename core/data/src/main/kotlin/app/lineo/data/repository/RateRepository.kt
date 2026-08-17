package app.lineo.data.repository

import app.lineo.data.model.CachedRate
import kotlinx.coroutines.flow.Flow

/**
 * The currency rate cache (`TASKS.md` P2-07).
 *
 * Phase 0 provides the cache only. Fetching arrives with `:feature:currency`, and the
 * implementation there is offline-first: a cached rate is always served, with its
 * [CachedRate.fetchedAt] date visible, rather than an empty state
 * (`docs/ARCHITECTURE.md` §3).
 */
interface RateRepository {

    fun getRateStream(base: String, quote: String): Flow<CachedRate?>

    fun getRatesStream(): Flow<List<CachedRate>>

    suspend fun cache(rates: List<CachedRate>)
}
