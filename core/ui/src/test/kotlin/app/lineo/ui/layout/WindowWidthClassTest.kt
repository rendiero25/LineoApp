package app.lineo.ui.layout

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The breakpoints, asserted on both sides of every boundary.
 *
 * An off-by-one here is invisible in review and visible on exactly one device size, so the
 * cases below sit at 599/600 and 839/840 rather than somewhere comfortably inside a band.
 */
class WindowWidthClassTest {

    @Test
    fun `a phone held upright is compact`() {
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.of(widthDp = 393f, heightDp = 851f))
    }

    @Test
    fun `one dp below the medium breakpoint is still compact`() {
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.of(widthDp = 599f, heightDp = 851f))
    }

    @Test
    fun `the medium breakpoint itself is medium`() {
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.of(widthDp = 600f, heightDp = 851f))
    }

    @Test
    fun `one dp below the expanded breakpoint is still medium`() {
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.of(widthDp = 839f, heightDp = 851f))
    }

    @Test
    fun `the expanded breakpoint itself is expanded`() {
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.of(widthDp = 840f, heightDp = 851f))
    }

    @Test
    fun `a tablet is expanded`() {
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.of(widthDp = 1280f, heightDp = 800f))
    }

    @Test
    fun `height does not decide the width class`() {
        // A phone in landscape is short and wide. Squeezing the height must not narrow the
        // layout — that is the branch-on-orientation mistake §2 forbids, arriving sideways.
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.of(widthDp = 800f, heightDp = 360f))
    }

    @Test
    fun `a phone in split screen is as narrow as a phone`() {
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.of(widthDp = 393f, heightDp = 400f))
    }
}
