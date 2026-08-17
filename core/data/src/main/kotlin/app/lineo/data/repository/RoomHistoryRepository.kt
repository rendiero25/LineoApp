package app.lineo.data.repository

import app.lineo.data.db.dao.HistoryDao
import app.lineo.data.db.entity.HistoryEntity
import app.lineo.data.model.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

internal class RoomHistoryRepository @Inject constructor(
    private val dao: HistoryDao,
    private val clock: Clock,
) : HistoryRepository {

    override fun getEntriesStream(limit: Int): Flow<List<HistoryEntry>> =
        dao.entriesStream(limit).map { entities -> entities.map { it.toModel() } }

    override suspend fun record(
        expression: String,
        resultText: String,
        moduleId: String?,
        limit: Int,
    ) {
        dao.insert(
            HistoryEntity(
                expression = expression,
                resultText = resultText,
                createdAt = clock.millis(),
                moduleId = moduleId,
            ),
        )
        dao.trimTo(limit)
    }

    override suspend fun clear() = dao.clear()
}

private fun HistoryEntity.toModel() = HistoryEntry(
    id = id,
    expression = expression,
    resultText = resultText,
    createdAt = Instant.ofEpochMilli(createdAt),
    moduleId = moduleId,
)
