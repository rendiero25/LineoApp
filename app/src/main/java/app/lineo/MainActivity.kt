package app.lineo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import app.lineo.shell.LineoAppShell
import app.lineo.shell.NotepadRoute
import app.lineo.shell.NotepadViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity host. Screens are navigation destinations; nothing else lives here.
 *
 * `enableEdgeToEdge()` before `setContent`, and no inset handling of its own — the window
 * is drawn behind the system bars and `LineoAppShell` decides what to keep clear of them.
 *
 * The `ViewModel` is taken with `by viewModels()` rather than `hiltViewModel()`: that
 * function lives in `hilt-navigation-compose`, which is not on the classpath, and adding a
 * dependency is a decision `AGENTS.md` §7 reserves for a human. There is one screen, so the
 * activity is its owner and the document survives a rotation either way.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notepad: NotepadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LineoAppShell {
                NotepadRoute(viewModel = notepad)
            }
        }
    }
}
