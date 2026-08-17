package app.lineo.notepad

import app.lineo.engine.LineId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The conversion itself, away from the document that usually drives it.
 *
 * Everything here is about text: which characters are a reference, which are not, and what
 * happens to the characters around them. The document tests cover what the numbers *mean*.
 */
class LineReferencesTest {

    @Test
    fun `both forms of a reference are recognised, and each keeps the form it was written in`() {
        val ordinals = mapOf(LineId(7) to 1, LineId(9) to 2)

        val display = LineReferences.toDisplay("line7 + @9", ordinals::get)

        assertEquals("line1 + @2", display)
        assertEquals(listOf(LineId(7), LineId(9)), LineReferences.referencedIds("line7 + @9"))
    }

    @Test
    fun `text around a reference is left exactly as it was`() {
        val canonical = "  2 * line12 + sqrt(16) // twice line12\n"

        val display = LineReferences.toDisplay(canonical) { 3 }

        // Spacing, the function call and the comment are untouched; only the token changes,
        // and the reference inside the comment is not a reference at all.
        assertEquals("  2 * line3 + sqrt(16) // twice line12\n", display)
    }

    @Test
    fun `a number that merely looks like a reference is not one`() {
        val untouched = listOf("line", "12", "outline3", "line_3", "3line")

        untouched.forEach { source ->
            assertEquals(source, LineReferences.toDisplay(source) { 99 })
            assertEquals(emptyList<LineId>(), LineReferences.referencedIds(source))
        }
    }

    @Test
    fun `a reference repeated is rewritten every time it appears`() {
        val display = LineReferences.toDisplay("line4 + line4 * line4") { 2 }

        assertEquals("line2 + line2 * line2", display)
    }

    @Test
    fun `references of different widths do not disturb each other`() {
        // Rewriting front to back would shift the spans of everything after the first
        // replacement; this is the case where that shows.
        val ordinals = mapOf(LineId(1) to 100, LineId(200) to 2)

        val display = LineReferences.toDisplay("line1 + line200", ordinals::get)

        assertEquals("line100 + line2", display)
    }

    @Test
    fun `a target that does not exist becomes a reference nothing can resolve`() {
        assertEquals("line0 + @0", LineReferences.toDisplay("line7 + @8") { null })
        assertEquals("line0", LineReferences.toCanonical("line99") { null })
    }

    @Test
    fun `display and stored text convert back into each other`() {
        val ids = mapOf(1 to LineId(41), 2 to LineId(42))
        val ordinals = mapOf(LineId(41) to 1, LineId(42) to 2)

        val canonical = LineReferences.toCanonical("line1 + @2 * 3", ids::get)

        assertEquals("line41 + @42 * 3", canonical)
        assertEquals("line1 + @2 * 3", LineReferences.toDisplay(canonical, ordinals::get))
    }

    @Test
    fun `text with no reference in it is returned unchanged`() {
        val source = "12,5 km + 300 m"

        assertEquals(source, LineReferences.toDisplay(source) { 1 })
        assertEquals(source, LineReferences.toCanonical(source) { LineId(1) })
    }
}
