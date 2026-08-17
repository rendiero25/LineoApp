package app.lineo.ui.input

import app.cash.turbine.test
import app.lineo.registry.EditorCommand
import app.lineo.registry.InputSurface
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What P0-13 actually promises: both surfaces are `InputSurface`, and a press leaves as an
 * `EditorCommand` and as nothing else.
 *
 * The tests press keys through the public API rather than calling `emit`, because the thing
 * worth protecting is the mapping — that `×` sends `*` and not `×`, that `√` sends a call
 * and not a glyph the parser would reject.
 */
class InputSurfaceTest {

    @Test
    fun `the editor can collect either surface without knowing which`() = runTest {
        val keypad = KeypadState()
        val accessory = AccessoryRowState()
        // The upcast is the assertion: whatever is attached, the editor sees only this type.
        val attached: List<InputSurface> = listOf(keypad, accessory)

        attached[0].commands.test {
            keypad.press(keypad.key("1"))

            assertEquals(EditorCommand.InsertText("1"), awaitItem())
        }
        attached[1].commands.test {
            accessory.press(accessory.key("^"))

            assertEquals(EditorCommand.InsertText("^"), awaitItem())
        }
    }

    @Test
    fun `a digit press emits the digit`() = runTest {
        val keypad = KeypadState()

        keypad.commands.test {
            keypad.press(keypad.key("7"))

            assertEquals(EditorCommand.InsertText("7"), awaitItem())
        }
    }

    @Test
    fun `operator keys emit what the parser reads, not what the key shows`() = runTest {
        // docs/GRAMMAR.md accepts * and /, while §10 shows × and ÷. The glyph is display.
        val keypad = KeypadState()

        keypad.commands.test {
            keypad.press(keypad.key("×"))
            keypad.press(keypad.key("÷"))
            keypad.press(keypad.key("−"))

            assertEquals(EditorCommand.InsertText("*"), awaitItem())
            assertEquals(EditorCommand.InsertText("/"), awaitItem())
            assertEquals(EditorCommand.InsertText("-"), awaitItem())
        }
    }

    @Test
    fun `backspace and equals emit their own commands, not text`() = runTest {
        val keypad = KeypadState()

        keypad.commands.test {
            keypad.press(keypad.key("⌫"))
            keypad.press(keypad.key("="))

            assertEquals(EditorCommand.Backspace, awaitItem())
            assertEquals(EditorCommand.NewLine, awaitItem())
        }
    }

    @Test
    fun `all clear empties the line and does not touch the document`() = runTest {
        val keypad = KeypadState()

        keypad.commands.test {
            keypad.press(keypad.key("AC"))

            // ClearLine, not a document-wide clear: EditorCommand has no such variant, on
            // purpose, and this asserts the keypad cannot reach for one either.
            assertEquals(EditorCommand.ClearLine, awaitItem())
        }
    }

    @Test
    fun `a comma decimal separator moves the argument separator to a semicolon`() {
        // docs/CONVENTIONS.md §2. The two keys live on different surfaces since the keypad
        // went to four columns, but the rule is the same one: they must never be the same
        // glyph, or the user types the wrong one half the time.
        val dotKeypad = KeypadState(decimalSeparator = '.').rows.flatten().map { it.label }
        val dotAccessory = AccessoryRowState(decimalSeparator = '.').keys.map { it.label }
        val commaKeypad = KeypadState(decimalSeparator = ',').rows.flatten().map { it.label }
        val commaAccessory = AccessoryRowState(decimalSeparator = ',').keys.map { it.label }

        assertTrue(dotKeypad.toString(), "." in dotKeypad)
        assertTrue(dotAccessory.toString(), "," in dotAccessory)
        assertTrue(commaKeypad.toString(), "," in commaKeypad)
        assertTrue(commaAccessory.toString(), ";" in commaAccessory)
    }

    @Test
    fun `the bracket key inserts a matched pair rather than a single glyph`() {
        // One key instead of two is what buys the fourth column its room; WrapSelection is
        // the contract that makes it possible without the keypad knowing about carets.
        val keypad = KeypadState()

        assertEquals(
            EditorCommand.WrapSelection(open = "(", close = ")"),
            keypad.key("( )").command,
        )
    }

    @Test
    fun `no two keys ever show the same label, in either separator convention`() {
        listOf('.', ',').forEach { separator ->
            val labels = KeypadState(separator).rows.flatten().map { it.label } +
                AccessoryRowState(separator).keys.map { it.label }
            val duplicated = labels.groupingBy { it }.eachCount().filterValues { it > 1 }

            // The keypad and the row share glyphs by design; each on its own must not.
            val keypadLabels = KeypadState(separator).rows.flatten().map { it.label }
            assertEquals(
                "separator $separator",
                emptyMap<String, Int>(),
                keypadLabels.groupingBy { it }.eachCount().filterValues { it > 1 },
            )
            assertTrue(duplicated.keys.all { it in keypadLabels })
        }
    }

    @Test
    fun `the decimal key types the separator it was given`() = runTest {
        val keypad = KeypadState(decimalSeparator = ',')

        keypad.commands.test {
            keypad.press(keypad.rows.last()[DECIMAL_KEY_COLUMN])

            assertEquals(EditorCommand.InsertText(","), awaitItem())
        }
    }

    @Test
    fun `changing the separator rebuilds the layout without replacing the surface`() {
        val keypad = KeypadState(decimalSeparator = '.')
        val before = keypad.commands

        keypad.decimalSeparator = ','

        assertEquals(",", keypad.rows.last()[DECIMAL_KEY_COLUMN].label)
        // Same flow instance: a collector attached before the change is still attached after.
        assertTrue(before === keypad.commands)
    }

    @Test
    fun `the square root key inserts a call, because the glyph alone does not parse`() = runTest {
        val accessory = AccessoryRowState()

        accessory.commands.test {
            accessory.press(accessory.key("√"))

            assertEquals(EditorCommand.InsertFunction(name = "sqrt", arity = 1), awaitItem())
        }
    }

    @Test
    fun `both surfaces toggle the text keyboard with the same command`() = runTest {
        // Aa raises the system keyboard and 123 brings the keypad back; one toggle, two keys.
        val keypad = KeypadState()
        val accessory = AccessoryRowState()

        assertEquals(EditorCommand.ToggleTextInput, keypad.key("Aa").command)
        assertEquals(EditorCommand.ToggleTextInput, accessory.key("123").command)
    }

    @Test
    fun `every key that shows a glyph rather than a word is described for TalkBack`() {
        val undescribed = (keypadRows('.').flatten() + accessoryKeys())
            .filter { it.contentDescription == null }
            .map { it.label }

        // Digits describe themselves; nothing else may.
        assertEquals(listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "0"), undescribed)
    }

    private fun KeypadState.key(label: String): KeypadKey =
        rows.flatten().single { it.label == label }

    private fun AccessoryRowState.key(label: String): KeypadKey =
        keys.single { it.label == label }

    private companion object {
        /** `Aa`, `0`, separator, `=` — the separator is the third column of the last row. */
        const val DECIMAL_KEY_COLUMN = 2
    }
}
