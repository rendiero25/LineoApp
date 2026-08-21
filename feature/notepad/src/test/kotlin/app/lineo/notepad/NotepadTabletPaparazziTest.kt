package app.lineo.notepad

import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.ui.layout.LocalWindowWidthClass
import app.lineo.ui.layout.WindowWidthClass
import app.lineo.ui.theme.LineoTheme
import org.junit.Rule
import org.junit.Test

/**
 * The notepad on a tablet (P1-08b).
 *
 * The width class is provided rather than measured: `LocalWindowWidthClass` is the shell's to
 * set from the real window, and a snapshot has no window to ask. Expanded is what a tablet
 * reports, and it is what turns on the keypad's fifth column — the layout `:feature:scientific`
 * already has a wide snapshot of and the notepad did not.
 */
class NotepadTabletPaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_C)

    @Test
    fun `notepad on a tablet`() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalWindowWidthClass provides WindowWidthClass.Expanded) {
                LineoTheme(dynamicColor = false) { NotepadScreen(state = notepad()) }
            }
        }
    }

    private fun notepad(): NotepadState {
        val state = NotepadState(Documents.of(*LINES.toTypedArray()))
        state.focus(state.uiState.value.lines.last().id)
        return state
    }

    private companion object {
        val LINES = listOf("rate = 12500", "5 km + 300 m", "3 * rate")
    }
}
