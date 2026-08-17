package app.lineo.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.lineo.data.db.LineoDatabase
import app.lineo.data.model.CURRENT_DOCUMENT_SCHEMA
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class RoomDocumentRepositoryTest {

    private lateinit var database: LineoDatabase
    private lateinit var repository: DocumentRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            LineoDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomDocumentRepository(database.documentDao(), database.lineDao(), FIXED_CLOCK)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `a new document carries the current content schema version`() = runTest {
        val id = repository.createDocument("Budget")

        val document = repository.getDocumentStream(id).first()

        assertEquals("Budget", document?.title)
        assertEquals(CURRENT_DOCUMENT_SCHEMA, document?.schemaVersion)
    }

    @Test
    fun `appended lines get consecutive ordinals`() = runTest {
        val id = repository.createDocument("Budget")

        repository.appendLine(id, "1 + 1")
        repository.appendLine(id, "line1 * 2")

        assertEquals(listOf(0, 1), repository.getLinesStream(id).first().map { it.ordinal })
    }

    @Test
    fun `reordering changes ordinals and keeps every line id`() = runTest {
        val id = repository.createDocument("Budget")
        val first = repository.appendLine(id, "1 + 1")
        val second = repository.appendLine(id, "line1 * 2")

        repository.reorderLines(id, listOf(second, first))

        val lines = repository.getLinesStream(id).first()
        // docs/ARCHITECTURE.md §6: ordinal is display order, the id is what a reference binds to.
        assertEquals(listOf(second, first), lines.map { it.id })
        assertEquals(listOf(0, 1), lines.map { it.ordinal })
        assertEquals(listOf("line1 * 2", "1 + 1"), lines.map { it.source })
    }

    @Test
    fun `editing a line keeps its id`() = runTest {
        val id = repository.createDocument("Budget")
        val lineId = repository.appendLine(id, "1 + 1")
        val line = repository.getLinesStream(id).first().single()

        repository.updateLine(line.copy(source = "2 + 2"))

        val updated = repository.getLinesStream(id).first().single()
        assertEquals(lineId, updated.id)
        assertEquals("2 + 2", updated.source)
    }

    @Test
    fun `deleting a document deletes its lines`() = runTest {
        val id = repository.createDocument("Budget")
        repository.appendLine(id, "1 + 1")

        repository.deleteDocument(id)

        assertNull(repository.getDocumentStream(id).first())
        assertEquals(emptyList<Any>(), repository.getLinesStream(id).first())
    }

    private companion object {
        val FIXED_CLOCK: Clock = Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC)
    }
}
