package app.lineo.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.lineo.R
import app.lineo.data.settings.DECIMAL_PLACES_RANGE
import app.lineo.data.settings.SeparatorPreference
import app.lineo.data.settings.ThemePreference
import app.lineo.data.settings.UnitSystem
import app.lineo.data.settings.UserSettings
import app.lineo.engine.AngleMode
import app.lineo.ui.input.ChoiceChip
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.RoleColors

/**
 * Everything the user can change (P1-07).
 *
 * Stateless: it renders [settings] and reports choices through [actions]. Each row is a
 * label and a set of chips rather than a dialog, because every one of these settings has
 * three or four values and a chip row shows what the alternatives *are* — a dialog hides
 * them behind the current one.
 *
 * There is no "apply": a choice takes effect as it is made, which is what the definition of
 * done means by the keypad following the separator immediately.
 */
@Composable
internal fun SettingsScreen(
    settings: UserSettings,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RoleColors.of(LineoRole.Editor).container),
        contentPadding = PaddingValues(LineoDimens.EditorPadding),
        verticalArrangement = Arrangement.spacedBy(LineoDimens.LineGap),
    ) {
        item { CalculationSettings(settings = settings, actions = actions) }
        item { AppearanceSettings(settings = settings, actions = actions) }
        item {
            LinkRow(
                title = stringResource(R.string.settings_licences),
                subtitle = stringResource(R.string.settings_licences_subtitle),
                onClick = actions.onOpenLicences,
            )
        }
    }
}

/**
 * How numbers are read and shown: the separator, the angle mode, and how many decimals.
 */
@Composable
private fun CalculationSettings(settings: UserSettings, actions: SettingsActions) {
    // A Column, because a lazy item that emits several children stacks them in one slot.
    Column(verticalArrangement = Arrangement.spacedBy(LineoDimens.LineGap)) {
        ChoiceRow(
            title = stringResource(R.string.settings_separator),
            options = SeparatorPreference.entries,
            selected = settings.separator,
            label = { stringResource(separatorLabel(it)) },
            onSelect = actions.onSeparator,
        )
        ChoiceRow(
            title = stringResource(R.string.settings_angle_mode),
            options = AngleMode.entries,
            selected = settings.angleMode,
            label = { it.name },
            onSelect = actions.onAngleMode,
        )
        ChoiceRow(
            title = stringResource(R.string.settings_decimal_places),
            options = DECIMAL_PLACES_RANGE.toList(),
            selected = settings.decimalPlaces,
            label = { it.toString() },
            onSelect = actions.onDecimalPlaces,
        )
    }
}

/**
 * What the app looks like, and which units it offers first.
 */
@Composable
private fun AppearanceSettings(settings: UserSettings, actions: SettingsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(LineoDimens.LineGap)) {
        ChoiceRow(
            title = stringResource(R.string.settings_theme),
            options = ThemePreference.entries,
            selected = settings.theme,
            label = { stringResource(themeLabel(it)) },
            onSelect = actions.onTheme,
        )
        ChoiceRow(
            title = stringResource(R.string.settings_unit_system),
            options = UnitSystem.entries,
            selected = settings.unitSystem,
            label = { stringResource(unitSystemLabel(it)) },
            onSelect = actions.onUnitSystem,
        )
        SwitchRow(
            title = stringResource(R.string.settings_dynamic_color),
            subtitle = stringResource(R.string.settings_dynamic_color_subtitle),
            checked = settings.dynamicColor,
            onChange = actions.onDynamicColor,
        )
    }
}

/**
 * What the settings screen can ask for.
 *
 * One object rather than seven parameters, so adding a setting does not re-thread the call
 * site — and so a preview or a test can pass a single do-nothing instance.
 */
internal data class SettingsActions(
    val onSeparator: (SeparatorPreference) -> Unit,
    val onAngleMode: (AngleMode) -> Unit,
    val onTheme: (ThemePreference) -> Unit,
    val onUnitSystem: (UnitSystem) -> Unit,
    val onDecimalPlaces: (Int) -> Unit,
    val onDynamicColor: (Boolean) -> Unit,
    val onOpenLicences: () -> Unit,
)

/** A label and the values it can take, the current one filled in. */
@Composable
private fun <T> ChoiceRow(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = RoleColors.of(LineoRole.Editor).content,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                // The row is one group of options, so TalkBack announces the title once and
                // then walks the chips, instead of leaving each chip to introduce itself.
                .semantics { this.selectableGroup() }
                .padding(top = LineoDimens.Grid),
            horizontalArrangement = Arrangement.spacedBy(LineoDimens.KeyGap),
        ) {
            options.forEach { option ->
                ChoiceChip(
                    label = label(option),
                    selected = option == selected,
                    onSelect = { onSelect(option) },
                    // "1,5" and "1.5" say nothing out loud on their own, and neither does a
                    // bare number of decimal places: each chip is read as an answer to the
                    // question its title asked.
                    description = "$title: ${label(option)}",
                )
            }
        }
    }
}

/** A setting that is on or off. The only one, and the toggle says which way round it is. */
@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ChipCornerRadius))
            .clickable { onChange(!checked) }
            .padding(vertical = LineoDimens.Grid),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = RoleColors.of(LineoRole.Editor).content,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = RoleColors.of(LineoRole.Result).content,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** A row that opens another screen. */
@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ChipCornerRadius))
            .clickable(onClick = onClick)
            .padding(vertical = LineoDimens.LineGap),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = RoleColors.of(LineoRole.Editor).content,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = RoleColors.of(LineoRole.Result).content,
        )
    }
}

private fun separatorLabel(preference: SeparatorPreference): Int = when (preference) {
    SeparatorPreference.AUTO -> R.string.settings_separator_auto
    SeparatorPreference.DOT -> R.string.settings_separator_dot
    SeparatorPreference.COMMA -> R.string.settings_separator_comma
}

private fun themeLabel(preference: ThemePreference): Int = when (preference) {
    ThemePreference.SYSTEM -> R.string.settings_theme_system
    ThemePreference.LIGHT -> R.string.settings_theme_light
    ThemePreference.DARK -> R.string.settings_theme_dark
}

private fun unitSystemLabel(system: UnitSystem): Int = when (system) {
    UnitSystem.AUTO -> R.string.settings_unit_system_auto
    UnitSystem.METRIC -> R.string.settings_unit_system_metric
    UnitSystem.IMPERIAL -> R.string.settings_unit_system_imperial
}

private val ChipCornerRadius = 12.dp
