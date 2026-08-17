package app.lineo.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.lineo.data.db.LineoDatabase
import app.lineo.data.model.CachedRate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class RoomRateRepositoryTest {

    private lateinit var database: LineoDatabase
    private lateinit var repository: RateRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            LineoDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomRateRepository(database.rateDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `a rate survives storage without losing a digit`() = runTest {
        // 20 significant digits: a REAL column would round this, DECIMAL128 does not.
        val exact = BigDecimal("16342.12345678901234567")
        repository.cache(listOf(rate(quote = "IDR", value = exact)))

        val stored = repository.getRateStream("USD", "IDR").first()

        assertEquals(exact, stored?.rate)
    }

    @Test
    fun `caching the same pair twice replaces the row`() = runTest {
        repository.cache(listOf(rate(quote = "IDR", value = BigDecimal("16000"))))
        repository.cache(listOf(rate(quote = "IDR", value = BigDecimal("16500"))))

        assertEquals(1, repository.getRatesStream().first().size)
        assertEquals(BigDecimal("16500"), repository.getRateStream("USD", "IDR").first()?.rate)
    }

    @Test
    fun `an uncached pair is absent rather than an error`() = runTest {
        assertEquals(null, repository.getRateStream("USD", "JPY").first())
    }

    private fun rate(quote: String, value: BigDecimal) = CachedRate(
        base = "USD",
        quote = quote,
        rate = value,
        fetchedAt = Instant.parse("2026-08-17T00:00:00Z"),
        source = "test",
    )
}
