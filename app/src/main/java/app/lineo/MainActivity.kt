package app.lineo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import app.lineo.engine.EvalContext
import app.lineo.engine.unit.UnitRegistry
import app.lineo.registry.ModuleRegistry
import app.lineo.registry.Tier
import app.lineo.shell.LineoAppShell
import app.lineo.shell.ModuleMenu
import app.lineo.shell.ModuleRoute
import app.lineo.shell.NotepadRoute
import app.lineo.shell.NotepadViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single activity host. Screens are navigation destinations; nothing else lives here.
 *
 * `enableEdgeToEdge()` before `setContent`, and no inset handling of its own — the window
 * is drawn behind the system bars and `LineoAppShell` decides what to keep clear of them.
 *
 * The `ViewModel` is taken with `by viewModels()` rather than `hiltViewModel()`: that
 * function lives in `hilt-navigation-compose`, which is not on the classpath, and adding a
 * dependency is a decision `AGENTS.md` §7 reserves for a human. The notepad's `ViewModel`
 * belongs to the activity, so opening a module and coming back does not re-read the document.
 *
 * Which destination is showing is one `rememberSaveable` module id rather than a nav graph.
 * **Not a navigation library**: which one Lineo adopts is the open decision recorded against
 * P0-01, due before P1-10, and P1-04-2 needed a second destination before that answer exists.
 * An id survives process death, a module still never sees a controller
 * (`docs/ARCHITECTURE.md` §4), and whatever wins later is an edit to this file alone.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notepad: NotepadViewModel by viewModels()

    @Inject
    lateinit var modules: ModuleRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Entitlement is FREE until Play Billing lands at P2-01. The scientific module is
            // free anyway (`docs/SPEC.md` §4), so nothing is hidden by the placeholder.
            val locale = LocalConfiguration.current.locales[0]
            val visible = modules.visible(Tier.FREE, locale)
            var openModuleId: String? by rememberSaveable { mutableStateOf(null) }
            val open = visible.firstOrNull { it.id == openModuleId }

            LineoAppShell(
                overflow = {
                    ModuleMenu(modules = visible, onOpenModule = { openModuleId = it })
                },
            ) {
                if (open == null) {
                    NotepadRoute(
                        viewModel = notepad,
                        // Every module's functions *and* units, so a name typed in the notepad
                        // resolves to the same thing its own screen calls (`AGENTS.md` §1):
                        // `sec(60)` from the scientific module, `5 km to mi` from the converter.
                        context = EvalContext(
                            locale = locale,
                            functions = modules.functionRegistry(Tier.FREE, locale),
                            units = UnitRegistry.BUILTIN.with(modules.units(Tier.FREE, locale)),
                        ),
                    )
                } else {
                    ModuleRoute(
                        module = open,
                        onLeave = { openModuleId = null },
                        onOpenModule = { id -> openModuleId = id },
                    )
                }
            }
        }
    }
}
