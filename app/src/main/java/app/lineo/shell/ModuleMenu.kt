package app.lineo.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.lineo.registry.CalculatorModule

/**
 * The overflow button, and what it opens.
 *
 * P0-11b-2 left a button that opened nothing, recorded as a defect that must not ship. This
 * is the first thing it opens: the calculator modules this build contains, taken from
 * `ModuleRegistry` rather than listed here — a module added later appears without this file
 * being edited, which is the point of the registry.
 *
 * History (P1-06) and settings (P1-07) join the same menu when they exist.
 *
 * @param modules the modules the user may see, already filtered by entitlement and locale.
 */
@Composable
internal fun ModuleMenu(
    modules: List<CalculatorModule>,
    onOpenModule: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OverflowMenuButton(onClick = { expanded = !expanded })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            modules.forEach { module ->
                DropdownMenuItem(
                    text = { Text(stringResource(module.titleRes)) },
                    onClick = {
                        expanded = false
                        onOpenModule(module.id)
                    },
                )
            }
        }
    }
}
