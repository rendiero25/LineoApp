package app.lineo.scientific

import androidx.compose.runtime.Composable
import app.lineo.registry.CalcFunction
import app.lineo.registry.CalculatorModule
import app.lineo.registry.LocaleGate
import app.lineo.registry.ModuleNav
import app.lineo.registry.Tier

/**
 * The scientific calculator, as a module (`docs/ARCHITECTURE.md` §4).
 *
 * Two faces of one capability: [functions] is what can be typed anywhere, [Screen] is the
 * focused surface that types it. No arithmetic happens in the screen — it inserts calls into
 * an expression and the engine evaluates them, so everything reachable here is reachable
 * from notepad mode by typing, which is the constraint `AGENTS.md` §1 puts on every module.
 *
 * Free, and unconditionally offered: `docs/SPEC.md` §4 puts full scientific calculation in
 * the free tier, and there is nothing regional about a sine.
 *
 * It carries no injection annotation and takes no dependencies. `:app` contributes it to the
 * `Set<CalculatorModule>` multibinding, which is the only place that knows the full set — so
 * a feature never has to depend on the DI framework to be part of the build.
 */
class ScientificModule : CalculatorModule {

    override val id: String = "scientific"

    override val titleRes: Int = R.string.scientific_title

    override val iconRes: Int = R.drawable.ic_scientific

    override val tier: Tier = Tier.FREE

    override val locales: LocaleGate = LocaleGate.All

    /** The gap between what the engine already has and a scientific calculator. See [ScientificFunctions]. */
    override fun functions(): List<CalcFunction> = ScientificFunctions.ALL

    /**
     * [nav] is deliberately untouched.
     *
     * The screen has nothing to navigate to, and back belongs to the host that placed it —
     * see [ScientificScreen]. The parameter stays because the contract has it and the next
     * module will need it.
     */
    @Composable
    override fun Screen(nav: ModuleNav) {
        ScientificScreen()
    }
}
