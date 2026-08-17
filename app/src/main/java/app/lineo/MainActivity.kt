package app.lineo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.lineo.shell.LineoAppShell
import app.lineo.shell.SingleLineScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity host. Screens are navigation destinations; nothing else lives here.
 *
 * `enableEdgeToEdge()` before `setContent`, and no inset handling of its own — the window
 * is drawn behind the system bars and `LineoAppShell` decides what to keep clear of them.
 *
 * One line, evaluated end to end. `:feature:notepad` replaces it at P1-03.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LineoAppShell {
                SingleLineScreen()
            }
        }
    }
}
