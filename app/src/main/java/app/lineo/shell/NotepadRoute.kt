package app.lineo.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lineo.R
import app.lineo.notepad.NotepadScreen

/**
 * The notepad, opened from storage and kept there.
 *
 * Everything this adds to [NotepadScreen] is lifecycle: when to read the document, and when
 * to make sure the last keystroke has been written. The screen itself knows nothing about
 * storage, which is what lets it be snapshot in `:feature:notepad` with no database at all.
 *
 * Nothing is rendered while the first read is in flight. An empty notepad is a real state —
 * the first run — and showing one for the length of a disk read would put the caret in a line
 * that is about to be replaced by the user's own.
 */
@Composable
internal fun NotepadRoute(viewModel: NotepadViewModel) {
    val title = stringResource(R.string.notepad_default_title)
    LaunchedEffect(viewModel) { viewModel.open(title) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        // ON_STOP and not ON_PAUSE: pause fires for a dialog over the screen, where nothing
        // is at risk, while stop is the last event guaranteed before the process can be
        // killed. Back, the recents switcher and the home gesture all reach it.
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.flush()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notepad by viewModel.notepad.collectAsStateWithLifecycle()
    notepad?.let { NotepadScreen(state = it.state) }
}
