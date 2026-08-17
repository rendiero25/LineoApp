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
    fun `the decimal key types the separator it was given`() = runTest {
        val keypad = KeypadState(decimalSeparator = ',')

        keypad.commands.test {
            keypad.press(keypad.key(","))

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
