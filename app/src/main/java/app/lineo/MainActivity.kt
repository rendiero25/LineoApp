package app.lineo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.lineo.billing.BillingRepository
import app.lineo.billing.Entitlement
import app.lineo.data.settings.ThemePreference
import app.lineo.engine.EvalContext
import app.lineo.engine.unit.UnitRegistry
import app.lineo.registry.CalculatorModule
import app.lineo.registry.ModuleRegistry
import app.lineo.registry.Tier
import app.lineo.shell.Destination
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

    @Inject
    lateinit var billing: BillingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LineoApp(modules, billing, notepad, history, settings) }
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
    billing: BillingRepository,
    notepad: NotepadViewModel,
    history: HistoryViewModel,
    settings: SettingsViewModel,
) {
    val systemLocale = LocalConfiguration.current.locales[0]
    val stored by settings.uiState.collectAsStateWithLifecycle()
    val entitlement by billing.entitlement.collectAsStateWithLifecycle()

    // One resolution for the whole window: the same locale reads the numbers, prints them and
    // labels the decimal key, so the three can never disagree.
    val resolved = remember(stored, systemLocale) { ResolvedSettings.of(stored, systemLocale) }
    val locale = resolved.locale
    val tier = when (entitlement) {
        Entitlement.Free -> Tier.FREE
        Entitlement.Premium -> Tier.PREMIUM
    }
    val visible = modules.visible(tier, locale)
    val navController = rememberNavController()

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
                navController = navController,
            )
        },
    ) {
        LineoNavHost(
            navController = navController,
            visible = visible,
            resolved = resolved,
            // Every module's functions *and* units, so a name typed in the notepad resolves to
            // the same thing its own screen calls (`AGENTS.md` §1): `sec(60)` from the scientific
            // module, `5 km to mi` from the converter. The angle mode and the reading locale come
            // from the settings, which is what makes them mean anything.
            context = EvalContext(
                locale = locale,
                angleMode = resolved.angleMode,
                functions = modules.functionRegistry(tier, locale),
                units = UnitRegistry.BUILTIN.with(modules.units(tier, locale)),
            ),
            notepad = notepad,
            history = history,
            settings = settings,
        )
    }
}

/**
 * The destinations, and what each one is handed.
 *
 * A function of its own rather than a block inside `LineoApp`: what the app resolves — the
 * settings, the tier, the module list — and what the graph does with them are two different
 * readings, and only the second one changes when a destination is added.
 */
@Composable
private fun LineoNavHost(
    navController: NavHostController,
    visible: List<CalculatorModule>,
    resolved: ResolvedSettings,
    context: EvalContext,
    notepad: NotepadViewModel,
    history: HistoryViewModel,
    settings: SettingsViewModel,
) {
    NavHost(navController = navController, startDestination = Destination.Notepad) {
        composable<Destination.Notepad> {
            NotepadRoute(viewModel = notepad, context = context)
        }

        composable<Destination.History> {
            HistoryRoute(
                viewModel = history,
                // Reuse lands in the notepad, so the tape leaves the module behind as well as
                // itself: the expression goes where a line can hold it.
                onReuse = { expression ->
                    navController.popBackStack(Destination.Notepad, inclusive = false)
                    notepad.reuse(expression)
                },
                onLeave = { navController.popBackStack() },
            )
        }

        composable<Destination.Settings> {
            SettingsRoute(
                viewModel = settings,
                onLeave = { navController.popBackStack() },
            )
        }

        composable<Destination.Module> { backStackEntry ->
            val route: Destination.Module = backStackEntry.toRoute()
            val open = visible.firstOrNull { it.id == route.id }
            if (open != null) {
                ModuleRoute(
                    module = open,
                    // Already resolved, never AUTO: what "auto" means is a locale question, and
                    // `ResolvedSettings` is where every locale question is answered.
                    unitSystem = resolved.unitSystem,
                    onLeave = { navController.popBackStack() },
                    onOpenModule = { id ->
                        navController.navigate(Destination.Module(id)) {
                            popUpTo(Destination.Notepad)
                        }
                    },
                )
            }
        }
    }
}
