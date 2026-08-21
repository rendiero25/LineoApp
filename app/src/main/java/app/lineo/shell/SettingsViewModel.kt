package app.lineo.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lineo.data.settings.SeparatorPreference
import app.lineo.data.settings.SettingsRepository
import app.lineo.data.settings.ThemePreference
import app.lineo.data.settings.UnitSystem
import app.lineo.data.settings.UserSettings
import app.lineo.engine.AngleMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The settings, and the only way to change them (P1-07).
 *
 * One `uiState`, as `docs/ANDROID_STANDARDS.md` §1 requires, and it is the stored settings
 * themselves: nothing here is derived, so there is nothing that could disagree with what is
 * on disk. Every setter writes and returns; the new value arrives back through the same flow
 * the screen renders from, which is what makes "changing the separator updates the keypad
 * immediately" true without an event.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<UserSettings> = repository
        .getSettingsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UserSettings())

    fun setSeparator(separator: SeparatorPreference) = update { repository.setSeparator(separator) }

    fun setAngleMode(mode: AngleMode) = update { repository.setAngleMode(mode) }

    fun setTheme(theme: ThemePreference) = update { repository.setTheme(theme) }

    fun setUnitSystem(system: UnitSystem) = update { repository.setUnitSystem(system) }

    fun setDecimalPlaces(places: Int) = update { repository.setDecimalPlaces(places) }

    fun setDynamicColor(enabled: Boolean) = update { repository.setDynamicColor(enabled) }

    private fun update(write: suspend () -> Unit) {
        viewModelScope.launch { write() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
