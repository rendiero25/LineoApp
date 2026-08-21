package app.lineo.notepad

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.ui.theme.LineoTheme
import org.junit.Rule
import org.junit.Test

/**
 * The notepad at the largest font a user can ask for (P1-08b).
 *
 * `docs/ANDROID_STANDARDS.md` §2 requires the display and the keypad to survive the maximum
 * system font size without clipping. The document is the half at risk: an expression is
 * already the largest type in the app, and at 2× it either wraps, shrinks or runs off the
 * end of the line — and only a picture says which.
 */
class NotepadFontScalePaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(fontScale = MAXIMUM_FONT_SCALE))

    @Test
    fun `notepad at the maximum font scale`() {
        paparazzi.snapshot {
            LineoTheme(dynamicColor = false) { NotepadScreen(state = notepad()) }
        }
    }

    @Test
    fun `notepad at the maximum font scale with the text keyboard up`() {
        paparazzi.snapshot {
            LineoTheme(dynamicColor = false) { NotepadScreen(state = notepad(textInput = true)) }
        }
    }

    private fun notepad(textInput: Boolean = false): NotepadState {
        val state = NotepadState(Documents.of(*LINES.toTypedArray()))
        state.focus(state.uiState.value.lines.last().id)
        if (textInput) state.apply(app.lineo.registry.EditorCommand.ToggleTextInput)
        return state
    }

    private companion object {
        /** What the accessibility font-size slider reaches on a Pixel. */
        const val MAXIMUM_FONT_SCALE = 2f
        val LINES = listOf("rate = 12500", "5 km + 300 m", "3 * rate")
    }
}
