package app.lineo.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import app.lineo.engine.AngleMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * The repository is tested against an in-memory [DataStore] rather than a file.
 *
 * What this class contributes is the mapping — names to enums, absent keys to defaults, an
 * unreadable file to defaults. Persisting bytes is DataStore's own job and is already
 * covered by androidx; exercising it here would only test someone else's file locking.
 */
class DataStoreSettingsRepositoryTest {

    private val dataStore = FakePreferenceDataStore()
    private val repository: SettingsRepository = DataStoreSettingsRepository(dataStore)

    @Test
    fun `an empty store yields the documented defaults`() = runTest {
        assertEquals(UserSettings(), repository.getSettingsStream().first())
    }

    @Test
    fun `every setting round-trips`() = runTest {
        repository.setLocaleOverride("id-ID")
        repository.setAngleMode(AngleMode.RAD)
        repository.setTheme(ThemePreference.DARK)
        repository.setSeparator(SeparatorPreference.COMMA)

        assertEquals(
            UserSettings("id-ID", AngleMode.RAD, ThemePreference.DARK, SeparatorPreference.COMMA),
            repository.getSettingsStream().first(),
        )
    }

    @Test
    fun `clearing the locale override falls back to the system locale`() = runTest {
        repository.setLocaleOverride("de-DE")

        repository.setLocaleOverride(null)

        assertEquals(null, repository.getSettingsStream().first().localeOverride)
    }

    @Test
    fun `a value this build does not know falls back to the default`() = runTest {
        // What a settings file written by a newer build looks like to an older one.
        dataStore.edit { it[stringPreferencesKey("angle_mode")] = "TURNS" }

        assertEquals(AngleMode.DEG, repository.getSettingsStream().first().angleMode)
    }

    @Test
    fun `an unreadable settings file yields the defaults instead of failing`() = runTest {
        val unreadable = DataStoreSettingsRepository(FailingPreferenceDataStore())

        assertEquals(UserSettings(), unreadable.getSettingsStream().first())
    }
}

private class FakePreferenceDataStore : DataStore<Preferences> {

    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(
        transform: suspend (Preferences) -> Preferences,
    ): Preferences = transform(state.value).also { state.value = it }
}

/** Stands in for a settings file that cannot be read — a truncated write, or bad storage. */
private class FailingPreferenceDataStore : DataStore<Preferences> {

    override val data: Flow<Preferences> = flow { throw IOException("unreadable") }

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        throw IOException("unreadable")
}
