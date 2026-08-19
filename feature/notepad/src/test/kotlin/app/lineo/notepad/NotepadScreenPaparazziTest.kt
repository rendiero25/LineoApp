package app.lineo.notepad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.registry.EditorCommand
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.LineoTheme
import app.lineo.ui.theme.RoleColors
import org.junit.Rule
import org.junit.Test

/**
 * The notepad screen in the schemes and the direction §10 and P1-08 name.
 *
 * Two documents, because one screenful cannot hold every state a line can be in and a
 * snapshot of a document scrolled past its own errors would prove nothing. [WORKING] is the
 * ordinary case — a definition, a unit, a line reading a name from above — and [FAILING] is
 * the three ways a line can have no value: wrong, blocked by a line that is wrong, and
 * misspelled with a fix to offer.
 *
 * The chip row is in every snapshot, above both surfaces. That is the gap P0-13 left open —
 * `%`, `^`, `√` and the argument separator unreachable in keypad mode — and the thing most
 * likely to regress, since it is the one row that belongs to neither surface.
 */
class NotepadScreenPaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `notepad over the keypad`() {
        paparazzi.snapshot { Notepad(WORKING) }
    }

    @Test
    fun `notepad over the keypad in dark scheme`() {
        paparazzi.snapshot { Notepad(WORKING, dark = true) }
    }

    @Test
    fun `notepad right to left`() {
        paparazzi.snapshot { RightToLeft { Notepad(WORKING) } }
    }

    @Test
    fun `notepad with the text keyboard up`() {
        paparazzi.snapshot { Notepad(WORKING, textInput = true) }
    }

    @Test
    fun `notepad showing an error, a blocked line and a fix chip`() {
        paparazzi.snapshot { Notepad(FAILING) }
    }

    @Test
    fun `notepad showing an error in dark scheme`() {
        paparazzi.snapshot { Notepad(FAILING, dark = true) }
    }

    /**
     * The failing lines in RTL, as rows rather than as a whole screen.
     *
     * A screen always has a focused line, a focused line is a text field, and a `singleLine`
     * field in an RTL layout crashes layoutlib outright: `NoSuchMethodError:
     * Thread.setPosixNicenessInternal`, from a handler thread its scroll path starts.
     * `singleLine` is not negotiable — without it the keyboard's return key writes a newline
     * into a line, seen on a device — so the RTL coverage P1-08 asks for is taken one level
     * down, on the rows, which is how every line that is not being typed in is drawn anyway.
     */
    @Test
    fun `failing lines right to left`() {
        paparazzi.snapshot { RightToLeft { UnfocusedLines(FAILING) } }
    }

    @Composable
    private fun UnfocusedLines(lines: List<String>, dark: Boolean = false) {
        // Read outside the composition, as in `Notepad` above.
        val shown = evaluatedLines(lines)
        LineoTheme(darkTheme = dark, dynamicColor = false) {
            Column(modifier = Modifier.background(RoleColors.of(LineoRole.Editor).container)) {
                // caret = null on all of them: this is a line as it is read, not as it is typed.
                shown.forEach { line ->
                    NotepadLineRow(
                        line = line,
                        caret = null,
                        actions = NotepadLineActions(
                            focus = {},
                            setText = { _, _ -> },
                            newLine = {},
                            applySuggestion = {},
                            ordinalOf = { id -> shown.firstOrNull { it.id == id }?.ordinal },
                        ),
                    )
                }
            }
        }
    }

    @Composable
    private fun RightToLeft(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { content() }
    }

    @Composable
    private fun Notepad(
        lines: List<String>,
        dark: Boolean = false,
        textInput: Boolean = false,
        focusOrdinal: Int = lines.size,
    ) {
        // Built outside the composition: reading a StateFlow's value inside one is a lint
        // error, and rightly — in an app that read is a snapshot that never updates again.
        val state = notepad(lines, textInput, focusOrdinal)
        LineoTheme(darkTheme = dark, dynamicColor = false) {
            NotepadScreen(state = state)
        }
    }

    /** The lines of a document, evaluated, with no caret in any of them. */
    private fun evaluatedLines(lines: List<String>): List<NotepadLineUiState> =
        notepad(lines, textInput = false, focusOrdinal = 1).uiState.value.lines

    /** Focused on the last line by default, so the chip row has everything above it to offer. */
    private fun notepad(lines: List<String>, textInput: Boolean, focusOrdinal: Int): NotepadState {
        val state = NotepadState(Documents.of(*lines.toTypedArray()))
        state.focus(state.uiState.value.lines[focusOrdinal - 1].id)
        if (textInput) state.apply(EditorCommand.ToggleTextInput)
        return state
    }

    private companion object {
        val WORKING = listOf("rate = 12500", "5 km + 300 m", "3 * rate")
        val FAILING = listOf("1 km + 2 kg", "line1 + 1", "sni(1)")
    }
}
