package app.lineo.registry

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import app.lineo.engine.unit.UnitDefinition

/**
 * A focused calculator contributed to the app, per `docs/ARCHITECTURE.md` §4.
 *
 * A module has two faces and they must stay consistent: [Screen] is the focused surface,
 * [functions] is the same capability as text. Calculation logic lives only in the
 * functions — a `Screen` that computes something itself is unreachable from notepad mode,
 * which breaks the core design constraint of `AGENTS.md` §1.
 *
 * Modules are contributed through Hilt multibinding into a `Set<CalculatorModule>` and
 * collected by [ModuleRegistry]. Only `:app` knows the full set.
 */
interface CalculatorModule {

    /** Stable identity, used for navigation and history rows. Never localised. */
    val id: String

    @get:StringRes
    val titleRes: Int

    @get:DrawableRes
    val iconRes: Int

    val tier: Tier

    /** Which locales this module is offered in. Regional packs gate themselves here. */
    val locales: LocaleGate

    /** The functions this module injects into the engine. Callable as text in notepad mode. */
    fun functions(): List<CalcFunction>

    /** Unit symbols this module defines, e.g. `:feature:converter` contributing `KiB`. */
    fun units(): List<UnitDefinition> = emptyList()

    /** The focused surface. Collects input, calls this module's own [functions]. */
    @Composable
    fun Screen(nav: ModuleNav)
}

/** Free or paid. The gate itself lives in `:core:billing` (P2-01); this is only the label. */
enum class Tier {
    FREE,
    PREMIUM,
}

/**
 * Navigation a module screen may ask for, kept deliberately tiny.
 *
 * The module never sees a `NavController`: the navigation library is still an open
 * decision (`TASKS.md`, P0-01), and a module must not have to care which one wins.
 */
interface ModuleNav {

    /** Leaves this module, back to wherever the user came from. */
    fun back()

    /** Opens another module by its [CalculatorModule.id]. Unknown ids are ignored. */
    fun openModule(id: String)
}
