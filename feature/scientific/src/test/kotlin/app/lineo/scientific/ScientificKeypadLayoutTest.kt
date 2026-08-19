package app.lineo.scientific

import app.lineo.registry.EditorCommand
import app.lineo.ui.input.KeypadKey
import app.lineo.ui.input.basicKeypadRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The grid this module contributes, asserted as data rather than as pixels.
 *
 * What matters here is not how a key looks — `:core:ui` owns that and its own snapshots
 * prove it — but that the digits are the ones every other surface has, that a function key
 * inserts a call the parser accepts, and that the wider window is the only thing that adds
 * rows.
 */
class ScientificKeypadLayoutTest {

    @Test
    fun `the digits and operators are the ones every other surface has`() {
        val basic = basicKeypadRows('.', hasRoomForFunctions = false)

        val rows = ScientificKeypadLayout.rows('.', hasRoomForFunctions = false)

        assertEquals(basic, rows.takeLast(basic.size))
    }

    @Test
    fun `a wider window adds rows rather than replacing them`() {
        val narrow = ScientificKeypadLayout.rows('.', hasRoomForFunctions = false)
        val wide = ScientificKeypadLayout.rows('.', hasRoomForFunctions = true)

        assertTrue(wide.size > narrow.size)
        // The inverses and the reciprocals are what the room buys.
        assertTrue(wide.labels().containsAll(listOf("sin⁻¹", "cos⁻¹", "tan⁻¹", "sec", "csc", "cot")))
        assertTrue(narrow.labels().none { it in listOf("sin⁻¹", "sec", "csc", "cot") })
    }

    @Test
    fun `every function key inserts a call and not a glyph`() {
        val functionKeys = ScientificKeypadLayout.rows('.', hasRoomForFunctions = true)
            .flatten()
            .filter { it.command is EditorCommand.InsertFunction }

        val names = functionKeys.map { (it.command as EditorCommand.InsertFunction).name }
        // `sqrt` is here because √ alone does not parse; the rest are the module's own keys
        // and the built-ins a scientific keypad is expected to reach.
        assertTrue(names.containsAll(listOf("sin", "cos", "tan", "ln", "log", "sec", "csc", "cot", "fact")))
        assertTrue(names.all { it.isNotBlank() })
    }

    @Test
    fun `every function this module registers has a key in the wide layout`() {
        // The keypad is the module's other face (`docs/ARCHITECTURE.md` §4). A function with
        // no key is only reachable by typing it, which defeats the point of a focused screen.
        val onKeys = ScientificKeypadLayout.rows('.', hasRoomForFunctions = true)
            .flatten()
            .mapNotNull { (it.command as? EditorCommand.InsertFunction)?.name }
            .toSet()

        val missing = ScientificModule().functions().map { it.name }.filterNot { it in onKeys }

        // min, max, hypot, sign and trunc are typed, not pressed: they take two arguments or
        // read as words, and a key each would cost a row the trigonometry needs more.
        assertEquals(listOf("sign", "trunc", "min", "max", "hypot"), missing)
    }

    @Test
    fun `no two keys in one layout show the same label`() {
        listOf(true, false).forEach { hasRoom ->
            listOf('.', ',').forEach { separator ->
                val duplicated = ScientificKeypadLayout.rows(separator, hasRoom)
                    .flatten()
                    .groupingBy { it.label }
                    .eachCount()
                    .filterValues { it > 1 }

                assertEquals("room=$hasRoom separator=$separator", emptyMap<String, Int>(), duplicated)
            }
        }
    }

    @Test
    fun `every key this module adds is described for TalkBack`() {
        val undescribed = ScientificKeypadLayout.rows('.', hasRoomForFunctions = true)
            .flatten()
            .filter { it.contentDescription == null }
            .map { it.label }

        // Only the digits describe themselves, and they come from `:core:ui`.
        assertEquals(listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "0"), undescribed)
    }

    private fun List<List<KeypadKey>>.labels(): List<String> = flatten().map { it.label }
}
