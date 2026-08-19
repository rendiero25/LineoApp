package app.lineo.converter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lineo.engine.Engine
import app.lineo.engine.EvalContext
import app.lineo.engine.unit.UnitRegistry
import app.lineo.ui.format.QuantityFormat
import app.lineo.ui.input.Keypad
import app.lineo.ui.input.rememberKeypadState
import app.lineo.ui.layout.AdaptivePane
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.LineoTypography
import app.lineo.ui.theme.RoleColors
import app.lineo.ui.theme.asExpression

/**
 * The converter: a category, an amount, and the two units it is read in.
 *
 * The amount reads from the top and the conversion from the bottom, in the same alignment the
 * notepad uses — a result lines up with what it came from. Nothing here computes: the state
 * hands the engine `"1.5 km to mi"`, which is exactly what a user could type into a notepad
 * line, so the two surfaces cannot drift.
 */
@Composable
internal fun ConverterScreen(modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    var savedCategory by rememberSaveable { mutableStateOf(ConverterCategory.LENGTH.id) }
    val state = rememberConverterState(savedCategory)
    val keypad = rememberKeypadState()
    val format = remember(locale) { QuantityFormat(locale) }

    LaunchedEffect(keypad, state) { keypad.commands.collect(state::apply) }
    LaunchedEffect(state.category) { savedCategory = state.category.id }

    AdaptivePane(
        modifier = modifier,
        // Category row, amount, conversion and the two pickers. Taller than the default one
        // expression and its result, and on a compact phone the difference is the whole of
        // the picker row — measured, drawn and clipped to nothing before this was said.
        minDocumentHeight = MinDocumentHeight,
        document = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RoleColors.of(LineoRole.Editor).container)
                    .padding(LineoDimens.EditorPadding),
                horizontalAlignment = Alignment.End,
            ) {
                CategoryRow(state = state)
                Amount(state = state)
                Conversion(state = state, format = format)
                UnitPickers(state = state)
            }
        },
        input = { Keypad(state = keypad) },
    )
}

/** The categories, scrollable, with the current one filled in. */
@Composable
private fun CategoryRow(state: ConverterState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = LineoDimens.Grid),
        horizontalArrangement = Arrangement.spacedBy(LineoDimens.KeyGap),
    ) {
        ConverterCategory.entries.forEach { category ->
            val selected = category == state.category
            val role = if (selected) LineoRole.Equals else LineoRole.SuggestionChip
            Chip(
                label = stringResource(category.titleRes),
                role = role,
                onClick = { state.select(category) },
            )
        }
    }
}

/** What was typed, in the unit it was typed in. Empty until there is something to show. */
@Composable
private fun Amount(state: ConverterState) {
    Text(
        text = if (state.amount.text.isBlank()) "" else "${state.amount.text} ${state.from.label}",
        style = LineoTypography.Expression.asExpression(),
        color = RoleColors.of(LineoRole.Editor).content,
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The same amount in the other unit — the one line on this screen the engine produces. */
@Composable
private fun Conversion(state: ConverterState, format: QuantityFormat) {
    val result = state.result
    Text(
        text = result?.let { format.format(it).display() } ?: "",
        style = LineoTypography.Result.asExpression(),
        color = RoleColors.of(LineoRole.Result).content,
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** `from` and `to`, each a menu of the category's units, with the swap between them. */
@Composable
private fun UnitPickers(state: ConverterState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = LineoDimens.Grid),
        // Ended, with the numbers above them: the amount, the result and the unit that
        // produced it all read from the same edge.
        horizontalArrangement = Arrangement.spacedBy(LineoDimens.KeyGap, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UnitPicker(
            unit = state.from,
            units = state.category.units,
            description = stringResource(R.string.converter_from_description),
            onSelect = state::selectFrom,
        )
        Chip(
            label = SWAP,
            role = LineoRole.Function,
            onClick = state::swap,
            description = stringResource(R.string.converter_swap_description),
        )
        UnitPicker(
            unit = state.to,
            units = state.category.units,
            description = stringResource(R.string.converter_to_description),
            onSelect = state::selectTo,
        )
    }
}

@Composable
private fun UnitPicker(
    unit: ConverterUnit,
    units: List<ConverterUnit>,
    description: String,
    onSelect: (ConverterUnit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Chip(
            label = unit.label,
            role = LineoRole.Operator,
            onClick = { expanded = true },
            description = "$description ${unit.label}",
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEach { candidate ->
                DropdownMenuItem(
                    text = { Text(candidate.label) },
                    onClick = {
                        expanded = false
                        onSelect(candidate)
                    },
                )
            }
        }
    }
}

/**
 * The one shape this screen draws.
 *
 * Chip-sized rather than key-sized: none of these types anything into the amount — they
 * choose what the amount means — and `docs/CONVENTIONS.md` §10 keeps that distinction
 * visible, the same way `ABC` sits apart from the keypad grid.
 */
@Composable
private fun Chip(label: String, role: LineoRole, onClick: () -> Unit, description: String? = null) {
    val colors = RoleColors.of(role)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(ChipCornerRadius))
            .background(colors.container)
            .clickable(onClick = onClick)
            .defaultMinSize(minWidth = LineoDimens.MinTouchTarget, minHeight = LineoDimens.MinTouchTarget)
            .padding(horizontal = LineoDimens.LineGap)
            .then(if (description == null) Modifier else Modifier.semantics { contentDescription = description }),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = colors.content)
    }
}

/**
 * A state that can see this module's own units.
 *
 * `EvalContext` defaults to the built-in registry, and `KiB` is not in it — the screen would
 * offer a unit whose conversion is `UnknownIdentifier`. The registry is built from this module
 * alone, since its own units are the only ones on screen; `:app` builds the full one for the
 * notepad.
 */
@Composable
private fun rememberConverterState(categoryId: String): ConverterState {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) {
        val units = UnitRegistry.BUILTIN.with(ConverterUnits.ALL)
        ConverterState(
            category = ConverterCategory.of(categoryId),
            evaluate = { source -> Engine.evaluate(source, EvalContext(locale = locale, units = units)) },
        )
    }
}

private const val SWAP = "⇅"
private val ChipCornerRadius = 12.dp

/**
 * What the converter's document pane needs: the category chips, the amount, the conversion,
 * and the pickers, with the padding around them.
 */
private val MinDocumentHeight = 300.dp
