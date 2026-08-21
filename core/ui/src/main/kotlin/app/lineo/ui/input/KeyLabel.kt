package app.lineo.ui.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * What a key shows, resolved where there is a `Context` to resolve it with.
 *
 * Most keys carry their label as text, because most labels are notation: `7`, `×`, `sin⁻¹`
 * read the same in every language. The few that are words — `ABC`, `123` — carry a resource
 * id instead, and this is the one place that turns it into a string, so a layout stays a
 * pure function (`docs/ARCHITECTURE.md` §5).
 */
@Composable
fun keyLabel(key: KeypadKey): String = key.labelRes?.let { stringResource(it) } ?: key.label
