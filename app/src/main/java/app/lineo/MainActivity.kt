package app.lineo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.lineo.shell.LineoAppShell
import app.lineo.shell.Phase0InputHarness
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity host. Screens are navigation destinations; nothing else lives here.
 *
 * `enableEdgeToEdge()` before `setContent`, and no inset handling of its own — the window
 * is drawn behind the system bars and `LineoAppShell` decides what to keep clear of them.
 *
 * The content is the Phase 0 input harness until the editor (P0-14) replaces it.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LineoAppShell {
                Phase0InputHarness()
            }
        }
    }
}
