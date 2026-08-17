package app.lineo.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.lineo.data.db.LineoDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class RoomHistoryRepositoryTest {

    private lateinit var database: LineoDatabase
    private lateinit var repository: HistoryRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            LineoDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomHistoryRepository(database.historyDao(), FIXED_CLOCK)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `the newest entry is first`() = runTest {
        repository.record("1 + 1", "2")
        repository.record("2 + 2", "4")

        assertEquals(
            listOf("2 + 2", "1 + 1"),
            repository.getEntriesStream().first().map { it.expression },
        )
    }

    @Test
    fun `the tape is trimmed to the free tier cap on write`() = runTest {
        repeat(FREE_TIER_HISTORY_LIMIT + 5) { index -> repository.record("$index + 0", "$index") }

        // Read past the cap: the rows must be gone from the table, not merely unread.
        val entries = repository.getEntriesStream(limit = FREE_TIER_HISTORY_LIMIT * 2).first()

        assertEquals(FREE_TIER_HISTORY_LIMIT, entries.size)
        assertEquals("54 + 0", entries.first().expression)
    }

    @Test
    fun `clearing removes everything`() = runTest {
        repository.record("1 + 1", "2")

        repository.clear()

        assertEquals(emptyList<Any>(), repository.getEntriesStream().first())
    }

    private companion object {
        val FIXED_CLOCK: Clock = Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC)
    }
}
