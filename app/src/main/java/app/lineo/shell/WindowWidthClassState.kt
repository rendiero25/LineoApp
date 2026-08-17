package app.lineo.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import app.lineo.ui.layout.WindowWidthClass

/**
 * The current [WindowWidthClass], measured from the window the activity is actually in.
 *
 * Read from `LocalWindowInfo.containerSize` rather than from the display or the resources
 * configuration. The window is not the screen: in split-screen or in a desktop-windowing
 * session the app owns a fraction of it, and on a foldable the fraction changes as the
 * device opens. `containerSize` is snapshot state, so the class recomputes when the window
 * resizes — no configuration callback, and no activity recreation, which
 * `docs/ANDROID_STANDARDS.md` §2 warns must not be relied on for a foldable.
 *
 * This lives in `:app` because computing it is the shell's job. `:core:ui` consumes the
 * result through `LocalWindowWidthClass` and never asks where it came from.
 */
@Composable
fun rememberWindowWidthClass(): WindowWidthClass {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return remember(containerSize, density) {
        with(density) {
            WindowWidthClass.of(
                widthDp = containerSize.width.toDp().value,
                heightDp = containerSize.height.toDp().value,
            )
        }
    }
}
