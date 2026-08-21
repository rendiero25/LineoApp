package app.lineo.shell

import app.lineo.data.settings.SeparatorPreference
import app.lineo.data.settings.SettingsRepository
import app.lineo.data.settings.ThemePreference
import app.lineo.data.settings.UnitSystem
import app.lineo.data.settings.UserSettings
import app.lineo.engine.AngleMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The settings screen's model (P1-07).
 *
 * What matters is that a choice reaches storage and comes back through the same flow the
 * screen renders from — that round trip is what makes "the keypad follows the setting
 * immediately" true without an event channel, which `docs/ANDROID_STANDARDS.md` §1 forbids.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeSettingsRepository()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the defaults are what a fresh install shows`() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(repository)

        assertEquals(UserSettings(), viewModel.uiState.value)
    }

    @Test
    fun `every choice round-trips through storage`() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(repository)
        val collector = launch { viewModel.uiState.collect {} }

        viewModel.setSeparator(SeparatorPreference.COMMA)
        viewModel.setAngleMode(AngleMode.RAD)
        viewModel.setTheme(ThemePreference.DARK)
        viewModel.setUnitSystem(UnitSystem.IMPERIAL)
        viewModel.setDecimalPlaces(2)
        viewModel.setDynamicColor(true)
        advanceUntilIdle()

        assertEquals(
            UserSettings(
                angleMode = AngleMode.RAD,
                theme = ThemePreference.DARK,
                separator = SeparatorPreference.COMMA,
                dynamicColor = true,
                unitSystem = UnitSystem.IMPERIAL,
                decimalPlaces = 2,
            ),
            viewModel.uiState.value,
        )

        collector.cancel()
    }
}

/** Settings in memory. The clamping and the enum fallbacks are `:core:data`'s and tested there. */
private class FakeSettingsRepository : SettingsRepository {

    private val state = MutableStateFlow(UserSettings())

    override fun getSettingsStream(): Flow<UserSettings> = state

    override suspend fun setLocaleOverride(tag: String?) {
        state.value = state.value.copy(localeOverride = tag)
    }

    override suspend fun setAngleMode(mode: AngleMode) {
        state.value = state.value.copy(angleMode = mode)
    }

    override suspend fun setTheme(theme: ThemePreference) {
        state.value = state.value.copy(theme = theme)
    }

    override suspend fun setSeparator(separator: SeparatorPreference) {
        state.value = state.value.copy(separator = separator)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        state.value = state.value.copy(dynamicColor = enabled)
    }

    override suspend fun setUnitSystem(system: UnitSystem) {
        state.value = state.value.copy(unitSystem = system)
    }

    override suspend fun setDecimalPlaces(places: Int) {
        state.value = state.value.copy(decimalPlaces = places)
    }
}
