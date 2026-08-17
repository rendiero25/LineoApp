package app.lineo.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.ui.theme.LineoTheme
import org.junit.Rule
import org.junit.Test

/**
 * The four things the editor can be showing, in the schemes and directions §10 and P1-08
 * name.
 *
 * The unfinished case is here precisely because it renders nothing below the line: a
 * snapshot is the cheapest way to notice the day it starts rendering something.
 */
class ExpressionEditorPaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `editor showing a result`() {
        paparazzi.snapshot { Editor(line = "0.1 + 0.2") }
    }

    @Test
    fun `editor showing a result in dark scheme`() {
        paparazzi.snapshot { Editor(line = "12 km + 300 m", dark = true) }
    }

    @Test
    fun `editor showing an unfinished line renders nothing below it`() {
        paparazzi.snapshot { Editor(line = "5 +") }
    }

    @Test
    fun `editor showing an error with a fix chip`() {
        paparazzi.snapshot { Editor(line = "sni(1)") }
    }

    @Test
    fun `editor showing an error in dark scheme`() {
        paparazzi.snapshot { Editor(line = "1 km + 2 kg", dark = true) }
    }

    @Test
    fun `editor right to left`() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Editor(line = "sni(1)")
            }
        }
    }

    @Composable
    private fun Editor(line: String, dark: Boolean = false) {
        val state = EditorState().apply {
            setText(line)
            // Publishes synchronously: Paparazzi has no clock to advance past the debounce.
            evaluateAndPublish()
        }
        LineoTheme(darkTheme = dark, dynamicColor = false) {
            ExpressionEditor(state = state)
        }
    }
}
