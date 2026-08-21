package app.lineo.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.lineo.engine.AngleMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * [SettingsRepository] over Preferences DataStore.
 *
 * Enums are stored by name, not by ordinal: an ordinal silently changes meaning the moment
 * a constant is inserted, and a stored `1` would then read back as a different angle mode.
 * A name that no longer exists falls back to the default rather than throwing — a settings
 * file written by a newer build must not stop an older one from starting.
 *
 * An unreadable file yields the defaults for the same reason
 * (`docs/ARCHITECTURE.md` §6: storage degrades, it does not fail).
 */
internal class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun getSettingsStream(): Flow<UserSettings> = dataStore.data
        .catch { cause -> if (cause is IOException) emit(emptyPreferences()) else throw cause }
        .map { preferences ->
            UserSettings(
                localeOverride = preferences[LOCALE_OVERRIDE],
                angleMode = preferences[ANGLE_MODE].toEnum(AngleMode.DEG),
                theme = preferences[THEME].toEnum(ThemePreference.SYSTEM),
                separator = preferences[SEPARATOR].toEnum(SeparatorPreference.AUTO),
                dynamicColor = preferences[DYNAMIC_COLOR] ?: false,
                unitSystem = preferences[UNIT_SYSTEM].toEnum(UnitSystem.AUTO),
                decimalPlaces = (preferences[DECIMAL_PLACES] ?: DEFAULT_DECIMAL_PLACES)
                    .coerceIn(DECIMAL_PLACES_RANGE),
            )
        }

    override suspend fun setLocaleOverride(tag: String?) {
        dataStore.edit { preferences ->
            if (tag == null) preferences.remove(LOCALE_OVERRIDE) else preferences[LOCALE_OVERRIDE] = tag
        }
    }

    override suspend fun setAngleMode(mode: AngleMode) {
        dataStore.edit { it[ANGLE_MODE] = mode.name }
    }

    override suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[THEME] = theme.name }
    }

    override suspend fun setSeparator(separator: SeparatorPreference) {
        dataStore.edit { it[SEPARATOR] = separator.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setUnitSystem(system: UnitSystem) {
        dataStore.edit { it[UNIT_SYSTEM] = system.name }
    }

    override suspend fun setDecimalPlaces(places: Int) {
        dataStore.edit { it[DECIMAL_PLACES] = places.coerceIn(DECIMAL_PLACES_RANGE) }
    }

    private companion object {
        val LOCALE_OVERRIDE = stringPreferencesKey("locale_override")
        val ANGLE_MODE = stringPreferencesKey("angle_mode")
        val THEME = stringPreferencesKey("theme")
        val SEPARATOR = stringPreferencesKey("separator")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val DECIMAL_PLACES = intPreferencesKey("decimal_places")
    }
}

/** The stored name, or [fallback] when it is absent or names a constant this build lacks. */
private inline fun <reified T : Enum<T>> String?.toEnum(fallback: T): T =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: fallback
