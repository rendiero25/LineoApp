package app.lineo.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.lineo.R
import app.lineo.registry.CalculatorModule
import app.lineo.ui.theme.LineoDimens

/**
 * The overflow button, and what it opens.
 *
 * Upgraded to a Material 3 vertical menu with icons and a divider.
 */
@Composable
internal fun ModuleMenu(
    modules: List<CalculatorModule>,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OverflowMenuButton(onClick = { expanded = true })

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            // The menu ends where the keypad's trailing column ends. Anchored to the button
            // alone it ended at the window edge, a hand's width to the right of everything
            // else on screen; `KeypadEdge` is the margin every other surface keeps, so the
            // menu keeps it too. Negative because the menu opens to the leading side of its
            // anchor, and `DropdownMenu` mirrors the sign in a right-to-left layout.
            offset = DpOffset(x = -LineoDimens.KeypadEdge, y = 0.dp),
        ) {
            // History first: it is about the work already done.
            DropdownMenuItem(
                text = { Text(stringResource(R.string.history_title)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_history), contentDescription = null) },
                onClick = {
                    expanded = false
                    navController.navigate(Destination.History)
                }
            )

            HorizontalDivider()

            // Modules section
            modules.forEach { module ->
                DropdownMenuItem(
                    text = { Text(stringResource(module.titleRes)) },
                    leadingIcon = { Icon(painterResource(module.iconRes), contentDescription = null) },
                    onClick = {
                        expanded = false
                        navController.navigate(Destination.Module(module.id))
                    }
                )
            }

            HorizontalDivider()

            // Settings last.
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_title)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_settings), contentDescription = null) },
                onClick = {
                    expanded = false
                    navController.navigate(Destination.Settings)
                }
            )
        }
    }
}
