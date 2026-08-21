package app.lineo.data.settings

import app.lineo.engine.AngleMode
import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes [UserSettings].
 *
 * Each setter takes one value, because settings screens change one thing at a time and a
 * whole-object write would race between two toggles.
 */
interface SettingsRepository {

    fun getSettingsStream(): Flow<UserSettings>

    suspend fun setLocaleOverride(tag: String?)

    suspend fun setAngleMode(mode: AngleMode)

    suspend fun setTheme(theme: ThemePreference)

    suspend fun setSeparator(separator: SeparatorPreference)

    /** Wallpaper colours instead of Lineo's palette. Off by default (`docs/CONVENTIONS.md` §10). */
    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setUnitSystem(system: UnitSystem)

    /** Coerced into [DECIMAL_PLACES_RANGE], so a stored value from another build cannot widen it. */
    suspend fun setDecimalPlaces(places: Int)
}
