package app.lineo.shell

import app.lineo.data.settings.SeparatorPreference
import app.lineo.data.settings.UnitSystem
import app.lineo.data.settings.UserSettings
import app.lineo.engine.AngleMode
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * A user's settings turned into the values a screen actually needs.
 *
 * Settings are stored as choices — `AUTO`, `COMMA`, `SYSTEM` — and screens need values: a
 * locale to read numbers in, the character the decimal key types, how many decimals to show,
 * and whether the wallpaper decides the colours. This is the one place that translation
 * happens, so two screens cannot resolve the same preference differently.
 *
 * **A separator override is expressed as a reading locale.** The engine reads numbers per
 * `EvalContext.locale` (`NumberFormatProfile`), so "always a comma" has to mean "read this
 * expression the way a comma-decimal locale is read" — grouping, argument separator and all.
 * That is what the choice means anyway: someone who writes `1,5` also writes `1.234,5` and
 * expects `max(1;5)` to take two arguments. The alternative — a separator the engine is told
 * about separately from the locale — would let the two disagree, which is exactly the state
 * `docs/CONVENTIONS.md` §1 exists to prevent.
 */
data class ResolvedSettings(
    val locale: Locale,
    val decimalSeparator: Char,
    val decimalPlaces: Int,
    val dynamicColor: Boolean,
    val angleMode: AngleMode,
    val unitSystem: UnitSystem,
) {

    companion object {

        /**
         * What a shell composed outside the app sees: the platform's own conventions.
         *
         * A function rather than a constant, because the platform locale changes while the
         * app is running — a `val` would hold whichever locale was current when the class
         * was first touched.
         */
        fun default(): ResolvedSettings = of(UserSettings(), Locale.getDefault())

        /** A dot-decimal locale to fall back on when the user's own is not one. */
        private val DOT_LOCALE: Locale = Locale.US

        /** A comma-decimal locale, for the same reason. */
        private val COMMA_LOCALE: Locale = Locale.GERMANY

        /**
         * Resolves [settings] against [systemLocale].
         *
         * The locale override wins over the system, and the separator override wins over
         * both — but only when it has to: a user in `id-ID` who chooses "comma" keeps `id-ID`,
         * because it already reads commas, and keeps their own grouping with it.
         */
        fun of(settings: UserSettings, systemLocale: Locale): ResolvedSettings {
            val chosen = settings.localeOverride?.let(Locale::forLanguageTag) ?: systemLocale
            val locale = when (settings.separator) {
                SeparatorPreference.AUTO -> chosen
                SeparatorPreference.DOT -> if (separatorOf(chosen) == '.') chosen else DOT_LOCALE
                SeparatorPreference.COMMA -> if (separatorOf(chosen) == ',') chosen else COMMA_LOCALE
            }
            return ResolvedSettings(
                locale = locale,
                decimalSeparator = separatorOf(locale),
                decimalPlaces = settings.decimalPlaces,
                dynamicColor = settings.dynamicColor,
                // Carried through unresolved: DEG and RAD mean what they say, and no locale
                // or platform value can change either. It rides here so that a screen needs
                // one object to be told everything the settings decide for it.
                angleMode = settings.angleMode,
                // Against [chosen], not the reading locale: a separator override can move that
                // one to Germany to get a comma, and choosing a comma is not a statement about
                // whether the user weighs things in pounds.
                unitSystem = unitSystemFor(settings.unitSystem, chosen),
            )
        }

        /**
         * What `AUTO` means for a unit system: the country the numbers are read in.
         *
         * The United States writes customary units and everywhere else writes metric — the
         * three other customary holdouts read metric in every context a calculator serves.
         * Resolved here rather than in the screen that shows the pickers, because "auto" is a
         * question about the locale and the locale is the host's (`LocalUnitSystem` says so
         * too: `AUTO` never reaches a module).
         */
        private fun unitSystemFor(setting: UnitSystem, locale: Locale): UnitSystem = when (setting) {
            UnitSystem.AUTO -> if (locale.country == "US") UnitSystem.IMPERIAL else UnitSystem.METRIC
            else -> setting
        }

        private fun separatorOf(locale: Locale): Char =
            DecimalFormatSymbols.getInstance(locale).decimalSeparator
    }
}
