package app.lineo.shell

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The history tape, wired to what it can do.
 *
 * Everything this adds to [HistoryScreen] is where the entries come from and where a tap
 * goes: reusing an entry hands the expression to the notepad and leaves, because the tape is
 * a place you pass through rather than one you work in.
 */
@Composable
internal fun HistoryRoute(
    viewModel: HistoryViewModel,
    onReuse: (String) -> Unit,
    onLeave: () -> Unit,
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    BackHandler(onBack = onLeave)

    HistoryScreen(
        entries = entries,
        onReuse = { expression ->
            onReuse(expression)
            onLeave()
        },
        onClear = viewModel::clear,
    )
}
