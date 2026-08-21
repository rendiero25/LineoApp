package app.lineo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lineo.data.settings.ThemePreference
import app.lineo.engine.EvalContext
import app.lineo.engine.unit.UnitRegistry
import app.lineo.registry.ModuleRegistry
import app.lineo.registry.Tier
import app.lineo.shell.HistoryRoute
import app.lineo.shell.HistoryViewModel
import app.lineo.shell.LineoAppShell
import app.lineo.shell.ModuleMenu
import app.lineo.shell.ModuleRoute
import app.lineo.shell.NotepadRoute
import app.lineo.shell.NotepadViewModel
import app.lineo.shell.ResolvedSettings
import app.lineo.shell.SettingsRoute
import app.lineo.shell.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single activity host. Screens are navigation destinations; nothing else lives here.
 *
 * `enableEdgeToEdge()` before `setContent`, and no inset handling of its own — the window
 * is drawn behind the system bars and `LineoAppShell` decides what to keep clear of them.
 *
 * The `ViewModel`s are taken with `by viewModels()` rather than `hiltViewModel()`: that
 * function lives in `hilt-navigation-compose`, which is not on the classpath, and adding a
 * dependency is a decision `AGENTS.md` §7 reserves for a human. They belong to the activity,
 * so opening a module and coming back does not re-read the document.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notepad: NotepadViewModel by viewModels()
    private val history: HistoryViewModel by viewModels()
    private val settings: SettingsViewModel by viewModels()

    @Inject
    lateinit var modules: ModuleRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LineoApp(modules, notepad, history, settings) }
    }
}

/**
 * The whole app below the window: the settings resolved once, and one destination showing.
 *
 * Which destination that is is `rememberSaveable` state rather than a nav graph. **Not a
 * navigation library**: which one Lineo adopts is the open decision recorded against P0-01,
 * due before P1-10, and P1-04-2 needed a second destination before that answer exists. Each
 * flag survives process death, a module still never sees a controller
 * (`docs/ARCHITECTURE.md` §4), and whatever wins later is an edit to this file alone.
 *
 * Lifted out of `onCreate` so that what the activity does — draw edge to edge and hand over —
 * stays readable beside what the app does.
 */
@Composable
private fun LineoApp(
    modules: ModuleRegistry,
    notepad: NotepadViewModel,
    history: HistoryViewModel,
    settings: SettingsViewModel,
) {
    val systemLocale = LocalConfiguration.current.locales[0]
    val stored by settings.uiState.collectAsStateWithLifecycle()
    // One resolution for the whole window: the same locale reads the numbers, prints them and
    // labels the decimal key, so the three can never disagree.
    val resolved = remember(stored, systemLocale) { ResolvedSettings.of(stored, systemLocale) }
    val locale = resolved.locale
    // Entitlement is FREE until Play Billing lands at P2-01. Every module shipped so far is
    // free anyway (`docs/SPEC.md` §4), so nothing is hidden by the placeholder.
    val visible = modules.visible(Tier.FREE, locale)
    var openModuleId: String? by rememberSaveable { mutableStateOf(null) }
    var historyOpen: Boolean by rememberSaveable { mutableStateOf(false) }
    var settingsOpen: Boolean by rememberSaveable { mutableStateOf(false) }
    val open = visible.firstOrNull { it.id == openModuleId }

    LineoAppShell(
        settings = resolved,
        darkTheme = when (stored.theme) {
            ThemePreference.SYSTEM -> isSystemInDarkTheme()
            ThemePreference.LIGHT -> false
            ThemePreference.DARK -> true
        },
        overflow = {
            ModuleMenu(
                modules = visible,
                onOpenModule = { openModuleId = it },
                onOpenHistory = { historyOpen = true },
                onOpenSettings = { settingsOpen = true },
            )
        },
    ) {
        when {
            settingsOpen -> SettingsRoute(viewModel = settings, onLeave = { settingsOpen = false })

            historyOpen -> HistoryRoute(
                viewModel = history,
                // Reuse lands in the notepad, so the tape leaves the module behind as well as
                // itself: the expression goes where a line can hold it.
                onReuse = { expression ->
                    openModuleId = null
                    notepad.reuse(expression)
                },
                onLeave = { historyOpen = false },
            )

            open == null -> NotepadRoute(
                viewModel = notepad,
                // Every module's functions *and* units, so a name typed in the notepad resolves
                // to the same thing its own screen calls (`AGENTS.md` §1): `sec(60)` from the
                // scientific module, `5 km to mi` from the converter. The angle mode and the
                // reading locale come from the settings, which is what makes them mean anything.
                context = EvalContext(
                    locale = locale,
                    angleMode = resolved.angleMode,
                    functions = modules.functionRegistry(Tier.FREE, locale),
                    units = UnitRegistry.BUILTIN.with(modules.units(Tier.FREE, locale)),
                ),
            )

            else -> ModuleRoute(
                module = open,
                // Already resolved, never AUTO: what "auto" means is a locale question, and
                // `ResolvedSettings` is where every locale question is answered.
                unitSystem = resolved.unitSystem,
                onLeave = { openModuleId = null },
                onOpenModule = { id -> openModuleId = id },
            )
        }
    }
}
