package app.lineo.data.repository

import app.lineo.data.db.dao.RateDao
import app.lineo.data.db.entity.RateCacheEntity
import app.lineo.data.model.CachedRate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

internal class RoomRateRepository @Inject constructor(
    private val dao: RateDao,
) : RateRepository {

    override fun getRateStream(base: String, quote: String): Flow<CachedRate?> =
        dao.rateStream(base, quote).map { it?.toModel() }

    override fun getRatesStream(): Flow<List<CachedRate>> =
        dao.ratesStream().map { entities -> entities.map { it.toModel() } }

    override suspend fun cache(rates: List<CachedRate>) = dao.upsert(rates.map { it.toEntity() })
}

private fun RateCacheEntity.toModel() = CachedRate(
    base = base,
    quote = quote,
    rate = BigDecimal(rate),
    fetchedAt = Instant.ofEpochMilli(fetchedAt),
    source = source,
)

private fun CachedRate.toEntity() = RateCacheEntity(
    base = base,
    quote = quote,
    rate = rate.toPlainString(),
    fetchedAt = fetchedAt.toEpochMilli(),
    source = source,
)
