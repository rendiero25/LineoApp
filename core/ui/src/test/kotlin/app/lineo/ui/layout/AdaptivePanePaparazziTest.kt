package app.lineo.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.LineoTheme
import app.lineo.ui.theme.RoleColors
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test

/**
 * [AdaptivePane] at each width class.
 *
 * The width class is provided directly rather than derived from the device, which is the
 * point: if the layout ever starts reading a width of its own, these three snapshots stop
 * differing and the test says so. The device sizes are set to match anyway, so a reviewer
 * sees a picture that could be real.
 */
class AdaptivePanePaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun `adaptive pane at compact width`() {
        paparazzi.unsafeUpdateConfig(deviceConfig = windowOf(widthPx = 786, heightPx = 1702))
        paparazzi.snapshot { Panes(WindowWidthClass.Compact) }
    }

    @Test
    fun `adaptive pane at medium width`() {
        paparazzi.unsafeUpdateConfig(deviceConfig = windowOf(widthPx = 1400, heightPx = 1800))
        paparazzi.snapshot { Panes(WindowWidthClass.Medium) }
    }

    @Test
    fun `adaptive pane at expanded width`() {
        paparazzi.unsafeUpdateConfig(deviceConfig = windowOf(widthPx = 1800, heightPx = 1400))
        paparazzi.snapshot { Panes(WindowWidthClass.Expanded) }
    }

    /** A window of the given size at 2x density, so pixels divided by two are dp. */
    private fun windowOf(widthPx: Int, heightPx: Int): DeviceConfig = DeviceConfig.PIXEL_5.copy(
        screenWidth = widthPx,
        screenHeight = heightPx,
        density = Density.XHIGH,
    )

    @Composable
    private fun Panes(widthClass: WindowWidthClass) {
        CompositionLocalProvider(LocalWindowWidthClass provides widthClass) {
            LineoTheme(darkTheme = false, dynamicColor = false) {
                AdaptivePane(
                    document = { DocumentPane() },
                    input = { InputPane() },
                )
            }
        }
    }

    /** Fills whatever the layout leaves it, which is what a notepad does. */
    @Composable
    private fun DocumentPane() {
        val colors = RoleColors.of(LineoRole.Editor)
        Box(
            modifier = Modifier.fillMaxSize().background(colors.container),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "document", style = MaterialTheme.typography.titleMedium, color = colors.content)
        }
    }

    /**
     * Wraps its height, which is the contract [AdaptivePane] places on an input pane and
     * what a keypad does naturally. A pane that filled instead would take the window and
     * leave the document nothing — the first version of this test did exactly that, and the
     * snapshot showed an input pane and no document at all.
     */
    @Composable
    private fun InputPane() {
        val colors = RoleColors.of(LineoRole.Digit)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.container)
                .padding(vertical = KeypadSampleHeight),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "input", style = MaterialTheme.typography.titleMedium, color = colors.content)
        }
    }
}

/** Stands in for the height of a few rows of keys. */
private val KeypadSampleHeight = 48.dp
