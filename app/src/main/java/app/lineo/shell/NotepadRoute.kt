package app.lineo.shell

import androidx.activity.compose.ReportDrawnWhen
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
import app.lineo.engine.EvalContext
import app.lineo.notepad.NotepadScreen
import app.lineo.ui.format.LocalQuantityFormat
import app.lineo.ui.input.LocalDecimalSeparator

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
 *
 * @param context the locale, the functions and the units every line is read against. It is
 *   the caller that has the locale to filter both registries with, and the notepad that must
 *   resolve the same names a module's own screen does.
 */
@Composable
internal fun NotepadRoute(viewModel: NotepadViewModel, context: EvalContext) {
    val title = stringResource(R.string.notepad_default_title)
    // The display boundary the shell resolved, handed to the view model because the tape and
    // the `±` key need it and neither is composed. Read here, where the composition is.
    val display = LocalQuantityFormat.current
    val decimalSeparator = LocalDecimalSeparator.current
    LaunchedEffect(viewModel) { viewModel.open(title, context, display, decimalSeparator) }
    // A settings change reaches a document that is already open: the angle mode and the
    // reading locale are what every line was evaluated against, and they have just changed.
    LaunchedEffect(context, display, decimalSeparator) {
        viewModel.apply(context, display, decimalSeparator)
    }

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
    // Startup is over when the app is *usable*, not when it is visible: the document has been
    // read and there is a line to type on. `docs/ANDROID_STANDARDS.md` §3 asks for the report,
    // and Macrobenchmark measures `fullyDrawn` from it — without it, startup would be timed to
    // the first frame, which is a notepad with nothing in it yet.
    ReportDrawnWhen { notepad != null }
    notepad?.let { NotepadScreen(state = it.state) }
}
