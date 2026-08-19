package app.lineo.notepad

import app.lineo.engine.LineId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mapping between a document and its rows.
 *
 * The assertions that matter are about identity, not about text: what has to survive a save
 * and a load is which line a reference points at (`docs/ARCHITECTURE.md` §6). A test that
 * only checked the words would pass on a store that renumbered every line on the way to disk.
 */
class NotepadStoreTest {

    private val repository = FakeDocumentRepository()
    private val store = NotepadStore(repository)

    @Test
    fun `a document survives a save and a load`() = runTest {
        val id = repository.createDocument("notepad")
        val document = Documents.of("rate = 12500", "3 * rate", "line2 + 1")

        store.save(id, document)

        assertEquals(document.lines, store.open(id).lines)
    }

    @Test
    fun `a reference still points at the same line after a round trip`() = runTest {
        val id = repository.createDocument("notepad")
        val document = Documents.of("100", "200", "line1 + 1")
        store.save(id, document)

        val reopened = store.open(id)

        val target = reopened.lines.first().id
        assertEquals(listOf(target), reopened.referencesOf(reopened.lines.last().id))
        assertEquals("line1 + 1", reopened.displayTextOf(reopened.lines.last().id))
    }

    @Test
    fun `ordinals are written from position, and a line inserted above does not repoint`() = runTest {
        val id = repository.createDocument("notepad")
        val document = Documents.of("100", "line1 + 1")
        store.save(id, document)
        val referenced = document.lines.first().id

        store.save(id, document.insertAt(0, ""))
        val reopened = store.open(id)

        assertEquals(listOf(0, 1, 2), repository.getLinesStream(id).first().map { it.ordinal })
        // The reference now reads `line2` on screen, because its target has moved down. It is
        // still the same line: the id in the stored text never changed.
        assertEquals(listOf(referenced), reopened.referencesOf(reopened.lines.last().id))
        assertEquals("line2 + 1", reopened.displayTextOf(reopened.lines.last().id))
    }

    @Test
    fun `a deleted line loses its row`() = runTest {
        val id = repository.createDocument("notepad")
        val document = Documents.of("1", "2", "3")
        store.save(id, document)
        val removed = document.lines[1].id

        store.save(id, document.delete(removed))

        assertNull(repository.getLinesStream(id).first().firstOrNull { it.id == removed })
        assertEquals(2, store.open(id).lines.size)
    }

    @Test
    fun `only the lines that changed are written`() = runTest {
        val id = repository.createDocument("notepad")
        val document = Documents.of("1", "2", "3")
        store.save(id, document)
        repository.writes.clear()

        val edited = document.setDisplayText(document.lines[1].id, "22")
        store.save(id, edited)

        assertEquals(listOf("22"), repository.writes.map { it.source })
    }

    @Test
    fun `saving an unchanged document writes nothing`() = runTest {
        val id = repository.createDocument("notepad")
        val document = Documents.of("1", "2")
        store.save(id, document)
        repository.writes.clear()

        store.save(id, document)

        assertEquals(emptyList<String>(), repository.writes.map { it.source })
    }

    @Test
    fun `a second document does not hand out an id the first already owns`() = runTest {
        val first = repository.createDocument("first")
        store.save(first, Documents.of("1", "2", "3"))
        val highest = repository.getLinesStream(first).first().maxOf { it.id.value }

        val second = repository.createDocument("second")
        val fresh = store.open(second).append("9")

        assertTrue(fresh.lines.single().id.value > highest)
        // The fake refuses an id another document holds, exactly as the table's primary key
        // would. Saving is the assertion.
        store.save(second, fresh)
    }

    @Test
    fun `the first run creates the notepad, with a line to type on`() = runTest {
        val opened = store.openOrCreate("Notepad")

        assertEquals(1, repository.getDocumentsStream().first().size)
        assertEquals(1, opened.document.lines.size)
        assertEquals("", opened.document.lines.single().source)
    }

    @Test
    fun `the second run opens what the first one left`() = runTest {
        val first = store.openOrCreate("Notepad")
        store.save(first.id, first.document.setDisplayText(first.document.lines.single().id, "6 * 7"))

        val second = store.openOrCreate("Notepad")

        assertEquals(first.id, second.id)
        assertEquals(listOf("6 * 7"), second.document.lines.map { it.source })
        assertEquals(1, repository.getDocumentsStream().first().size)
    }

    @Test
    fun `an unknown document opens empty rather than failing`() = runTest {
        val document = store.open(documentId = 404)

        assertEquals(emptyList<NotepadLine>(), document.lines)
        assertEquals(LineId(NotepadDocument.FIRST_LINE_ID), document.append().lines.single().id)
    }
}
