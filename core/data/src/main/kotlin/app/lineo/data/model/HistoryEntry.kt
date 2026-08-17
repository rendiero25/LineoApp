package app.lineo.data.model

import java.time.Instant

/**
 * One row of the history tape (`TASKS.md` P1-06).
 *
 * [resultText] is stored already formatted, because history is a record of what the user
 * saw. Re-evaluating it later under a different angle mode or locale would rewrite the
 * past.
 */
data class HistoryEntry(
    val id: Long,
    val expression: String,
    val resultText: String,
    val createdAt: Instant,
    val moduleId: String? = null,
)
