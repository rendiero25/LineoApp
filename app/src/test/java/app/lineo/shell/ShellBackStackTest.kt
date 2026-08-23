package app.lineo.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules of the shell's back stack (P1-14).
 *
 * These are mostly about *back*, because that is where the subtlety is. The three booleans
 * this replaced were not three peers: `openModuleId` survived underneath history and
 * settings, so leaving settings returned to the module you opened them from. That was never
 * written down anywhere — it fell out of the order of a `when` — and the first test here is
 * the one that would have caught a rewrite losing it.
 */
class ShellBackStackTest {

    private val scientific = Destination.Module("scientific")
    private val converter = Destination.Module("converter")

    @Test
    fun `the notepad is the root and cannot be left behind`() {
        val stack = ShellBackStack()

        assertEquals(Destination.Notepad, stack.current)
        assertFalse("back at the root is the activity's business, not ours", stack.back())
    }

    @Test
    fun `leaving settings returns to the module they were opened from`() {
        val stack = ShellBackStack()
        stack.open(scientific)

        stack.open(Destination.Settings)
        assertEquals(Destination.Settings, stack.current)

        assertTrue(stack.back())
        assertEquals("this is the behaviour the three booleans had by accident", scientific, stack.current)
    }

    @Test
    fun `leaving a module returns to the notepad`() {
        val stack = ShellBackStack()
        stack.open(scientific)

        assertTrue(stack.back())
        assertEquals(Destination.Notepad, stack.current)
    }

    @Test
    fun `one module replaces another rather than stacking on it`() {
        val stack = ShellBackStack()
        stack.open(scientific)

        stack.open(converter)

        assertEquals(converter, stack.current)
        // A module opened from a module is a sideways move — the scientific keypad is not a
        // place you were on the way to the converter, and back should not walk through it.
        assertTrue(stack.back())
        assertEquals(Destination.Notepad, stack.current)
    }

    @Test
    fun `opening a module from settings leaves the settings behind`() {
        val stack = ShellBackStack()
        stack.open(Destination.Settings)

        stack.open(scientific)

        assertEquals(scientific, stack.current)
        assertTrue(stack.back())
        assertEquals(Destination.Notepad, stack.current)
    }

    @Test
    fun `history opened from settings goes back to settings`() {
        val stack = ShellBackStack()
        stack.open(Destination.Settings)

        stack.open(Destination.History)

        // The three booleans put history *under* settings, so leaving settings revealed a
        // screen the user had already left. Back now returns where they came from.
        assertTrue(stack.back())
        assertEquals(Destination.Settings, stack.current)
    }

    @Test
    fun `re-opening a destination already below does not grow the stack`() {
        val stack = ShellBackStack()
        stack.open(Destination.Settings)
        stack.open(Destination.History)

        stack.open(Destination.Settings)

        assertEquals(Destination.Settings, stack.current)
        // Popped back to it rather than pushed on top: otherwise a menu the user can reach
        // from every screen grows the stack without bound, and back becomes a long walk.
        assertTrue(stack.back())
        assertEquals(Destination.Notepad, stack.current)
    }

    @Test
    fun `opening the destination already showing changes nothing`() {
        val stack = ShellBackStack()
        stack.open(Destination.Settings)

        stack.open(Destination.Settings)

        assertEquals(Destination.Settings, stack.current)
        assertTrue(stack.back())
        assertEquals(Destination.Notepad, stack.current)
    }

    @Test
    fun `the notepad is reachable as a destination and empties the stack`() {
        val stack = ShellBackStack()
        stack.open(scientific)
        stack.open(Destination.History)

        stack.open(Destination.Notepad)

        assertEquals(Destination.Notepad, stack.current)
        assertFalse("reuse from the tape lands on the notepad, with nothing behind it", stack.back())
    }

    @Test
    fun `every destination survives a save and restore`() {
        val stack = ShellBackStack()
        stack.open(scientific)
        stack.open(Destination.Settings)

        val restored = ShellBackStack.restore(ShellBackStack.save(stack))

        assertEquals(Destination.Settings, restored.current)
        assertTrue(restored.back())
        assertEquals(scientific, restored.current)
        assertTrue(restored.back())
        assertEquals(Destination.Notepad, restored.current)
    }

    @Test
    fun `a module id containing the separator survives the round trip`() {
        val stack = ShellBackStack()
        // Nothing forbids a module id with a colon in it, and the saved form uses one.
        stack.open(Destination.Module("tax:id-ID"))

        val restored = ShellBackStack.restore(ShellBackStack.save(stack))

        assertEquals(Destination.Module("tax:id-ID"), restored.current)
    }

    @Test
    fun `a saved stack from a build that knew other destinations restores to the notepad`() {
        // Forward compatibility costs one line and buys never crashing on an upgrade: the
        // saved form outlives the build that wrote it.
        val restored = ShellBackStack.restore(listOf("notepad", "graphing"))

        assertEquals(Destination.Notepad, restored.current)
        assertFalse(restored.back())
    }

    @Test
    fun `an empty saved stack restores to the notepad`() {
        val restored = ShellBackStack.restore(emptyList())

        assertEquals(Destination.Notepad, restored.current)
        assertFalse(restored.back())
    }
}
