package app.lineo.ui.layout

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Every combination of the two insets a bottom-docked surface has to clear.
 *
 * The function under test is one line. That is the point: the requirement in
 * `docs/ANDROID_STANDARDS.md` §2 is one line too — the larger of the two, never the sum —
 * and the cheapest way to break it is to type `+`. These cases fail the moment somebody
 * does.
 */
class InputSurfaceInsetsTest {

    @Test
    fun `neither inset present leaves the surface where it is`() {
        assertEquals(0, combineDockedBottomInset(imeBottom = 0, navigationBarBottom = 0))
    }

    @Test
    fun `only the navigation bar present clears the navigation bar`() {
        assertEquals(NAVIGATION_BAR, combineDockedBottomInset(imeBottom = 0, navigationBarBottom = NAVIGATION_BAR))
    }

    @Test
    fun `only the keyboard present clears the keyboard`() {
        assertEquals(KEYBOARD, combineDockedBottomInset(imeBottom = KEYBOARD, navigationBarBottom = 0))
    }

    @Test
    fun `both present clears the larger, because the keyboard is drawn over the bar`() {
        val combined = combineDockedBottomInset(imeBottom = KEYBOARD, navigationBarBottom = NAVIGATION_BAR)

        assertEquals(KEYBOARD, combined)
        assertEquals(
            "the two insets overlap; adding them lifts the surface a navigation bar too far",
            false,
            combined == KEYBOARD + NAVIGATION_BAR,
        )
    }

    @Test
    fun `a navigation bar taller than the keyboard still wins`() {
        // Not a real phone, but the function must not assume which is larger — a floating or
        // split keyboard reports a small inset while the bar keeps its full height.
        assertEquals(NAVIGATION_BAR, combineDockedBottomInset(imeBottom = 12, navigationBarBottom = NAVIGATION_BAR))
    }

    private companion object {
        /** Gesture navigation bar on a typical device, in pixels. */
        const val NAVIGATION_BAR = 48

        /** A shown soft keyboard, in pixels. Always the taller of the two in practice. */
        const val KEYBOARD = 720
    }
}
