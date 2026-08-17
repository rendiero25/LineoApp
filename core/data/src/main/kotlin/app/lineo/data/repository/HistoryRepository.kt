package app.lineo.data.repository

import app.lineo.data.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

/** The history tape (`TASKS.md` P1-06). */
interface HistoryRepository {

    fun getEntriesStream(limit: Int = FREE_TIER_HISTORY_LIMIT): Flow<List<HistoryEntry>>

    /**
     * Records one evaluation and trims the tape back to [limit].
     *
     * Trimming on write rather than on read keeps the table bounded across process death,
     * which is where an unbounded tape would otherwise grow.
     */
    suspend fun record(
        expression: String,
        resultText: String,
        moduleId: String? = null,
        limit: Int = FREE_TIER_HISTORY_LIMIT,
    )

    suspend fun clear()
}

/** `TASKS.md` P1-06. Premium raises it; the free tier is capped here. */
const val FREE_TIER_HISTORY_LIMIT: Int = 50
