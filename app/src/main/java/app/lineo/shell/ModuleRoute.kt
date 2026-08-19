package app.lineo.shell

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import app.lineo.registry.CalculatorModule
import app.lineo.registry.ModuleNav

/**
 * A module's own screen, with the two things a module is not allowed to have.
 *
 * Back is one: intercepting it needs `activity-compose`, which `:app` has and a feature does
 * not, so the module is placed inside a `BackHandler` rather than containing one. Navigation
 * is the other — [ModuleNav] is the whole of what a module may ask for, and it is answered
 * here (`docs/ARCHITECTURE.md` §4).
 *
 * @param module the module to show.
 * @param onLeave where back goes, and where an unknown [ModuleNav.openModule] id lands.
 * @param onOpenModule opens another module by id. Unknown ids are the caller's to ignore.
 */
@Composable
internal fun ModuleRoute(
    module: CalculatorModule,
    onLeave: () -> Unit,
    onOpenModule: (String) -> Unit,
) {
    BackHandler(onBack = onLeave)
    module.Screen(
        nav = object : ModuleNav {
            override fun back() = onLeave()

            override fun openModule(id: String) = onOpenModule(id)
        },
    )
}
