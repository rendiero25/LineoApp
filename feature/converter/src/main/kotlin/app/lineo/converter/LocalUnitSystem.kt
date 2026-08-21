package app.lineo.converter

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import app.lineo.data.settings.UnitSystem

/**
 * Which family of units a fresh category starts on (`TASKS.md` P1-07).
 *
 * Provided by the host, because the preference is a setting and a module screen has no way to
 * read one: `CalculatorModule.Screen(nav)` takes nothing, deliberately, and a module that
 * reached for a repository would be a module that knows about storage.
 *
 * [UnitSystem.AUTO] never reaches here — the host resolves it against the locale first, since
 * the locale is the host's and "auto" means "whatever this user's country writes".
 */
val LocalUnitSystem: ProvidableCompositionLocal<UnitSystem> =
    staticCompositionLocalOf { UnitSystem.METRIC }
