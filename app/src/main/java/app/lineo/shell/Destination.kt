package app.lineo.shell

/**
 * A place the shell can be showing (P1-14).
 *
 * One value where there used to be three independent flags — `openModuleId`, `historyOpen`
 * and `settingsOpen` — ordered by a `when`. Nothing stopped two being true at once, and two
 * could be: `ModuleMenu` is drawn on *every* destination, so opening history from the
 * settings screen set both, and leaving settings then revealed a screen the user had already
 * walked away from. The illegal state is now unrepresentable rather than merely unreached.
 *
 * A `sealed interface` rather than an enum because one variant carries data
 * (`AGENTS.md` §5), and the module id is what makes it a destination rather than a category.
 */
internal sealed interface Destination {

    /** The document. The root of the stack, and where back eventually stops. */
    data object Notepad : Destination

    /** The tape of finished calculations. */
    data object History : Destination

    /** Settings, and the licences behind them. */
    data object Settings : Destination

    /**
     * A calculator module's own screen.
     *
     * @param id the module's [app.lineo.registry.CalculatorModule.id]. An id no visible
     *   module claims resolves to the notepad rather than to an error — a module can be
     *   gated by tier or locale, and a saved stack can outlive the entitlement that put it
     *   there.
     */
    data class Module(val id: String) : Destination
}
