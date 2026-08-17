package app.lineo.notepad

import app.lineo.engine.CalcError
import app.lineo.engine.CalcResult
import app.lineo.engine.Engine
import app.lineo.engine.EvalContext
import app.lineo.engine.LineId
import app.lineo.engine.Quantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What P1-01 promises: an identity that survives every structural edit, and an ordinal that
 * is only ever a way of showing it.
 *
 * Every document here is built by [document], which makes the ids differ from the ordinals
 * on purpose. That is not decoration. In a freshly built document the two numbers are equal,
 * so a test written against one of them passes just as happily on a model that stores
 * ordinals and rewrites them on every insert — the design `docs/GRAMMAR.md` §3.7 rules out.
 * Removing the id-to-ordinal conversion from the model was tried, and with matching numbers
 * the whole suite stayed green.
 */
class NotepadDocumentTest {

    @Test
    fun `inserting a line above leaves an existing reference pointing at the same content`() {
        // The definition of done, in the form the task states it.
        val base = document("100", "200", "300").append()
        val referring = base.lines.last().id
        val typed = base.setDisplayText(referring, "line3 * 2")
        val third = typed.idAtOrdinal(3)!!

        val withInsertion = typed.insertAt(0, "0")

        // What was typed named an ordinal; what is stored names the line.
        assertNotEquals("line3 * 2", typed.line(referring)?.source)
        assertEquals("line${third.value} * 2", typed.line(referring)?.source)
        assertEquals("300", withInsertion.line(third)?.source)
        assertEquals(listOf(third), withInsertion.referencesOf(referring))
        assertEquals("line${third.value} * 2", withInsertion.line(referring)?.source)
        assertEquals("line4 * 2", withInsertion.displayTextOf(referring))
    }

    @Test
    fun `an ordinal is a position, and it moves when the position does`() {
        val document = document("1", "2", "3")
        val first = document.idAtOrdinal(1)!!

        assertEquals(1, document.ordinalOf(first))
        assertEquals(3, document.insertAt(0, "0").insertAt(0, "-1").ordinalOf(first))
        assertEquals(3, document.move(0, 2).ordinalOf(first))
        assertNull(document.delete(first).ordinalOf(first))
    }

    @Test
    fun `moving a line changes what references display and nothing they store`() {
        val document = referringDocument("line1 + line2")
        val referring = document.lines.last().id
        val stored = document.line(referring)!!.source

        val reordered = document.move(0, 1)

        assertEquals(stored, reordered.line(referring)!!.source)
        assertEquals("line1 + line2", document.displayTextOf(referring))
        assertEquals("line2 + line1", reordered.displayTextOf(referring))
    }

    @Test
    fun `deleting a line leaves references to it dangling rather than repointing them`() {
        val document = referringDocument("line1 + 1")
        val first = document.idAtOrdinal(1)!!
        val referring = document.lines.last().id

        val without = document.delete(first)

        assertEquals(mapOf(referring to listOf(first)), without.danglingReferences())
        // What the user reads says the target is gone — it does not silently become the line
        // that took over ordinal 1.
        assertEquals("line0 + 1", without.displayTextOf(referring))
    }

    @Test
    fun `an id is never handed out again after its line is deleted`() {
        val document = document("10", "20")
        val second = document.idAtOrdinal(2)!!

        val reused = document.delete(second).append("30")

        assertTrue(reused.lines.none { it.id == second })
        assertTrue(reused.lines.last().id.value > second.value)
    }

    @Test
    fun `editing one line touches no other`() {
        val document = document("10", "20", "30")
        val second = document.idAtOrdinal(2)!!

        val edited = document.edit(second, "25")

        assertEquals(listOf("10", "25", "30"), edited.lines.map { it.source })
        assertEquals(document.lines.map { it.id }, edited.lines.map { it.id })
    }

    @Test
    fun `dependents are the lines that refer to a given one`() {
        var document = document("10", "20", "30", "40")
        val first = document.idAtOrdinal(1)!!
        val second = document.idAtOrdinal(2)!!
        document = document
            .setDisplayText(document.idAtOrdinal(3)!!, "line1 * 2")
            .setDisplayText(document.idAtOrdinal(4)!!, "line1 + line2")

        assertEquals(listOf(document.idAtOrdinal(3), document.idAtOrdinal(4)), document.dependentsOf(first))
        assertEquals(listOf(document.idAtOrdinal(4)), document.dependentsOf(second))
        assertEquals(emptyList<LineId>(), document.dependentsOf(document.idAtOrdinal(4)!!))
    }

    @Test
    fun `a reference the user typed binds to the line they were looking at, not to the ordinal`() {
        val document = referringDocument("line2 + 1")
        val typedOn = document.lines.last().id
        val second = document.idAtOrdinal(2)!!

        val reordered = document.move(1, 0)

        assertEquals(listOf(second), reordered.referencesOf(typedOn))
        assertEquals("line1 + 1", reordered.displayTextOf(typedOn))
    }

    @Test
    fun `the engine resolves stored text against results keyed by id`() {
        // The model's whole output is a string the engine reads. If stored text were not in
        // id form, this is where it would show, and nowhere earlier.
        val document = referringDocument("line1 + line2")
        val total = document.lines.last().id
        val results = document.lines.dropLast(1).associate { line -> line.id to Quantity.of(line.source) }

        val result = Engine.evaluate(
            source = document.line(total)!!.source,
            context = EvalContext(lineResults = results),
        )

        assertEquals("30", (result as CalcResult.Ok).value.canonicalString())
    }

    @Test
    fun `a dangling reference reaches the engine as an error, not as a wrong answer`() {
        val document = referringDocument("line1 + 1")
        val total = document.lines.last().id
        val remaining = document.delete(document.idAtOrdinal(1)!!)

        val result = Engine.evaluate(
            source = remaining.displayTextOf(total)!!,
            context = EvalContext(lineResults = mapOf(remaining.lines.first().id to Quantity.of("20"))),
        )

        val error = (result as CalcResult.Err).error
        assertTrue(error.toString(), error is CalcError.UnknownIdentifier)
        assertEquals("line0", (error as CalcError.UnknownIdentifier).name)
    }

    @Test
    fun `a document rejects lines whose ids it could hand out again`() {
        val collision = runCatching {
            NotepadDocument(lines = listOf(NotepadLine(LineId(7), "1")), nextLineId = 7)
        }
        val duplicate = runCatching {
            NotepadDocument(
                lines = listOf(NotepadLine(LineId(1), "1"), NotepadLine(LineId(1), "2")),
                nextLineId = 2,
            )
        }

        assertTrue(collision.exceptionOrNull() is IllegalArgumentException)
        assertTrue(duplicate.exceptionOrNull() is IllegalArgumentException)
    }

    /**
     * A document holding [sources], whose ids deliberately do not match its ordinals.
     *
     * Two lines are added and deleted first, so the line displayed at ordinal 1 has id 3.
     * Any assertion that would accept an ordinal where an id belongs now fails.
     */
    private fun document(vararg sources: String): NotepadDocument {
        val padded = NotepadDocument.of(*(arrayOf("pad", "pad") + sources))
        return padded.delete(padded.idAtOrdinal(1)!!).delete(padded.idAtOrdinal(2)!!)
    }

    /** `10`, `20`, and a third line holding [typed] as the user wrote it. */
    private fun referringDocument(typed: String): NotepadDocument {
        val base = document("10", "20").append()
        return base.setDisplayText(base.lines.last().id, typed)
    }
}
