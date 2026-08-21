package app.lineo.shell

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Settings, and the licences screen behind them.
 *
 * Back closes the licences first and the settings second, so the gesture unwinds the screens
 * in the order they were opened rather than dropping the user out of both at once.
 */
@Composable
internal fun SettingsRoute(viewModel: SettingsViewModel, onLeave: () -> Unit) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    var licencesOpen by rememberSaveable { mutableStateOf(false) }

    BackHandler { if (licencesOpen) licencesOpen = false else onLeave() }

    if (licencesOpen) {
        LicencesScreen()
    } else {
        SettingsScreen(
            settings = settings,
            actions = remember(viewModel) {
                SettingsActions(
                    onSeparator = viewModel::setSeparator,
                    onAngleMode = viewModel::setAngleMode,
                    onTheme = viewModel::setTheme,
                    onUnitSystem = viewModel::setUnitSystem,
                    onDecimalPlaces = viewModel::setDecimalPlaces,
                    onDynamicColor = viewModel::setDynamicColor,
                    onOpenLicences = { licencesOpen = true },
                )
            },
        )
    }
}
