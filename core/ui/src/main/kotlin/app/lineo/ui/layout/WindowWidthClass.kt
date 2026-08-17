package app.lineo.ui.layout

import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass

/**
 * How much horizontal room Lineo has, in the only terms layout is allowed to reason about.
 *
 * `docs/ANDROID_STANDARDS.md` §2 forbids branching on device type, orientation, or a raw
 * width. A phone in split-screen is as narrow as a phone; a foldable is compact shut and
 * expanded open, without the activity being recreated in between. Only the window's own
 * measurement describes any of that, so it is the only thing worth branching on.
 *
 * The width in dp appears in exactly one place — [of] — and nowhere else in the app.
 *
 * | Class | Lineo layout |
 * |---|---|
 * | [Compact] | Single pane. Keypad or system keyboard docked at the bottom |
 * | [Medium] | Notepad with a persistent keypad panel; wider result column |
 * | [Expanded] | Two pane — document leading, keypad and module output trailing |
 */
enum class WindowWidthClass {

    /** Below 600 dp. A phone held upright, or any window squeezed to that width. */
    Compact,

    /** From 600 dp. A small tablet, a large phone in landscape, a half-screen window. */
    Medium,

    /** From 840 dp. A tablet, an unfolded foldable, a desktop window. */
    Expanded,

    ;

    companion object {

        /** Maps an androidx window size class onto the three cases Lineo lays out for. */
        fun of(windowSizeClass: WindowSizeClass): WindowWidthClass = when {
            windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> Expanded
            windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> Medium
            else -> Compact
        }

        /**
         * Computes the class from a window measured in dp.
         *
         * The one place a width is a number. Everything downstream sees a [WindowWidthClass].
         */
        fun of(widthDp: Float, heightDp: Float): WindowWidthClass =
            of(WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(widthDp, heightDp))
    }
}
