package app.lineo.ui.input

import app.cash.turbine.test
import app.lineo.registry.EditorCommand
import app.lineo.registry.InputSurface
import app.lineo.ui.R
import app.lineo.ui.theme.LineoRole
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
            accessory.press(accessory.key("^").command)

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
    fun `brackets are still reachable, on the accessory row`() {
        // They left the keypad when % took the slot. Nothing on the keypad types a bracket
        // any more, which is recorded as an open row in TASKS.md — this asserts the only
        // place that still can.
        val accessory = AccessoryRowState()

        assertEquals(EditorCommand.InsertText("("), accessory.key("(").command)
        assertEquals(EditorCommand.InsertText(")"), accessory.key(")").command)
    }

    @Test
    fun `the sign key toggles rather than typing a minus`() {
        val keypad = KeypadState()

        assertEquals(EditorCommand.ToggleSign, keypad.key("±").command)
    }

    @Test
    fun `a wider window grows a fifth column of function keys`() {
        // Not landscape — width. A tablet upright and an unfolded foldable get it too, and
        // docs/ANDROID_STANDARDS.md §2 forbids asking about orientation at all.
        val narrow = KeypadState(hasRoomForFunctions = false)
        val wide = KeypadState(hasRoomForFunctions = true)

        assertTrue(narrow.rows.all { it.size == 4 })
        assertTrue(wide.rows.all { it.size == 5 })
        assertTrue(wide.rows.flatten().map { it.label }.containsAll(listOf("(", ")", "^", "√")))
        // Operators keep the trailing edge; the new column is inserted before them.
        assertEquals(listOf("÷", "×", "−", "+", "="), wide.rows.map { it.last().label })
    }

    @Test
    fun `a supplied layout replaces the grid and still speaks commands`() = runTest {
        // What P1-04 needs from `:core:ui`: a module brings its own keys, and everything
        // else about a keypad — the separator, the extra width, the command stream — is
        // unchanged. The keys themselves stay in the module that owns them.
        val layout = KeypadLayout { separator, hasRoom ->
            listOf(listOf(KeypadKey("ln", LineoRole.Function, EditorCommand.InsertFunction("ln", 1)))) +
                basicKeypadRows(separator, hasRoom)
        }
        val keypad = KeypadState(decimalSeparator = ',', hasRoomForFunctions = true, layout = layout)

        assertEquals("ln", keypad.rows.first().single().label)
        assertEquals(",", keypad.rows.last()[DECIMAL_KEY_COLUMN].label)
        keypad.commands.test {
            keypad.press(keypad.key("ln"))

            assertEquals(EditorCommand.InsertFunction("ln", 1), awaitItem())
        }
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
            accessory.press(accessory.key("√").command)

            assertEquals(EditorCommand.InsertFunction(name = "sqrt", arity = 1), awaitItem())
        }
    }

    @Test
    fun `both surfaces toggle the text keyboard with the same command`() = runTest {
        // Aa raises the system keyboard and 123 brings the keypad back; one toggle, two keys.
        val keypad = KeypadState()
        val accessory = AccessoryRowState()

        assertEquals(EditorCommand.ToggleTextInput, keypad.modeKey.command)
        // Both labels are *words* — a script and a number system — so they are resource ids
        // rather than text (`AGENTS.md` §5), and the switch is asserted by what it emits.
        assertEquals(R.string.key_text_keyboard_label, keypad.modeKey.labelRes)
        val back = accessory.keys.single { it.command == EditorCommand.ToggleTextInput }
        assertEquals(R.string.key_numeric_keypad_label, back.labelRes)
    }

    @Test
    fun `every key that shows a glyph rather than a word is described for TalkBack`() {
        val undescribed = (KeypadState().allKeys() + accessoryKeys())
            .filter { it.contentDescription == null }
            .map { it.label }

        // Digits describe themselves; nothing else may.
        assertEquals(listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "0"), undescribed)
    }

    private fun KeypadState.key(label: String): KeypadKey =
        rows.flatten().single { it.label == label }

    private fun KeypadState.allKeys(): List<KeypadKey> = rows.flatten() + modeKey

    private fun AccessoryRowState.key(label: String): KeypadKey =
        keys.single { it.label == label }

    private companion object {
        /** `Aa`, `0`, separator, `=` — the separator is the third column of the last row. */
        const val DECIMAL_KEY_COLUMN = 2
    }
}
