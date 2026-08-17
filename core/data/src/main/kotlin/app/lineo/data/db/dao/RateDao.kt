package app.lineo.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.lineo.data.db.entity.RateCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface RateDao {

    @Query("SELECT * FROM rate_cache WHERE base = :base AND quote = :quote")
    fun rateStream(base: String, quote: String): Flow<RateCacheEntity?>

    @Query("SELECT * FROM rate_cache")
    fun ratesStream(): Flow<List<RateCacheEntity>>

    @Upsert
    suspend fun upsert(rates: List<RateCacheEntity>)
}
