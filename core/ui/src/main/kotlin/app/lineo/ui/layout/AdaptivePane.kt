package app.lineo.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.lineo.ui.theme.LineoDimens
import app.lineo.ui.theme.LineoRole
import app.lineo.ui.theme.RoleColors

/**
 * Lays a document pane and an input pane out according to the current [WindowWidthClass].
 *
 * The arrangement per class is the table in `docs/ANDROID_STANDARDS.md` §2, and it is the
 * only place in the app that decides it. Nothing here reads a width: the class arrives
 * through [LocalWindowWidthClass], which the shell provides and a fold changes at runtime
 * without the activity being recreated.
 *
 * **The input pane must size itself to its content.** It is measured before the document,
 * which then takes whatever is left, so an input that asks for `fillMaxHeight` or
 * `fillMaxSize` takes the window and leaves the document nothing. This is the same contract
 * Material's own `Scaffold` places on its `bottomBar`, and for the same reason. A keypad has
 * an intrinsic height — rows of keys — so meeting it is the natural thing to do anyway.
 *
 * @param document the notepad or the module output — whatever the user is reading. It is
 *   given the remaining space and may fill it.
 * @param input the keypad, the accessory row, or the system keyboard's placeholder. Must
 *   wrap its height.
 */
@Composable
fun AdaptivePane(
    document: @Composable () -> Unit,
    input: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (LocalWindowWidthClass.current) {
        WindowWidthClass.Compact, WindowWidthClass.Medium -> StackedPanes(document, input, modifier)
        WindowWidthClass.Expanded -> SideBySidePanes(document, input, modifier)
    }
}

/**
 * Document above, input docked below.
 *
 * Compact and medium share this shape. They differ in how much room the input pane is given
 * by its own content — a medium window fits a persistent keypad panel where a compact one
 * shows a single row — which is the input composable's decision, not this layout's.
 */
@Composable
private fun StackedPanes(
    document: @Composable () -> Unit,
    input: @Composable () -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(RoleColors.of(LineoRole.Editor).container)) {
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) { document() }
        Column(modifier = Modifier.fillMaxWidth().dockedBottomPadding()) { input() }
    }
}

/** Document leading, input trailing. The two-pane layout of an unfolded device or a tablet. */
@Composable
private fun SideBySidePanes(
    document: @Composable () -> Unit,
    input: @Composable () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(RoleColors.of(LineoRole.Editor).container),
    ) {
        Column(modifier = Modifier.weight(DOCUMENT_SHARE)) { document() }
        Column(
            modifier = Modifier
                .weight(INPUT_SHARE)
                .padding(start = LineoDimens.EditorPadding)
                .dockedBottomPadding(),
        ) { input() }
    }
}

/**
 * How the width is divided in the two-pane layout.
 *
 * The document takes the larger share: it is what the user reads, and a notepad line that
 * wraps is harder to scan than a keypad that is one key narrower.
 */
private const val DOCUMENT_SHARE = 0.55f
private const val INPUT_SHARE = 0.45f
