package app.lineo.ui.input

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lineo.registry.EditorCommand
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How big a key is, and the one size it may never go below (`docs/CONVENTIONS.md` §8).
 *
 * The snapshots assert what the keypad *looks* like; this asserts the rule underneath, in
 * the windows a snapshot does not cover — a short landscape pane, a six-row scientific grid.
 */
class KeySizeTest {

    private val basic = KeypadLayout.Basic.rows(
        decimalSeparator = '.',
        hasExtraColumn = false,
        hasRoomForFunctions = false,
    )

    @Test
    fun `a comfortable window is measured from the width`() {
        // Four columns, three 6 dp gaps: (400 - 18) / 4.
        assertEquals(95.5.dp, keySize(basic, maxWidth = 400.dp, maxHeight = 600.dp))
    }

    @Test
    fun `a short window is measured from the height instead`() {
        val size = keySize(basic, maxWidth = 800.dp, maxHeight = 400.dp)

        assertTrue("$size should be smaller than the width alone would allow", size < 195.dp)
    }

    @Test
    fun `no window makes a key too small to hit`() {
        // A landscape phone with the keyboard up: what is left is nowhere near five rows.
        assertEquals(LineoDimens.MinTouchTarget, keySize(basic, maxWidth = 700.dp, maxHeight = 200.dp))
    }

    @Test
    fun `an unbounded pane respects the minimum too`() {
        assertEquals(LineoDimens.MinTouchTarget, keySize(basic, maxWidth = 100.dp, maxHeight = Dp.Infinity))
    }

    @Test
    fun `a taller grid is floored at the same size`() {
        // The shape a scientific layout has: six rows of five.
        val scientific = List(6) { row ->
            List(5) { column ->
                KeypadKey(
                    label = "$row$column",
                    role = LineoRole.Digit,
                    command = EditorCommand.InsertText("$column"),
                )
            }
        }

        assertTrue(keySize(scientific, maxWidth = 320.dp, maxHeight = 300.dp) >= LineoDimens.MinTouchTarget)
    }
}
