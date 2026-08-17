package app.lineo.ui.layout

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The current [WindowWidthClass], provided once by the activity shell.
 *
 * A composition local rather than a parameter threaded through every screen: the width class
 * is ambient, every level of the tree may want it, and passing it by hand would put a
 * layout concern into the signature of components that only forward it.
 *
 * It is *static*: a change re-composes everything that reads it rather than tracking readers
 * individually. That is the cheaper trade here, because the value changes only when the
 * window is resized — a fold, a split-screen drag — and when it does, the layout is
 * rearranging anyway.
 *
 * The default is [WindowWidthClass.Compact]. A screen previewed or snapshot without a shell
 * around it renders as the narrowest case, which is the one most likely to break.
 */
val LocalWindowWidthClass: ProvidableCompositionLocal<WindowWidthClass> =
    staticCompositionLocalOf { WindowWidthClass.Compact }
