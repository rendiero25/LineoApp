package app.lineo.ui.editor

import app.lineo.engine.Quantity
import app.lineo.engine.ok
import app.lineo.registry.EditorCommand
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

/**
 * The 400 ms debounce, on a test scheduler rather than a stopwatch.
 *
 * Here the engine *is* faked, because what is being measured is how often it gets called.
 * A real engine would still pass, and would tell us nothing about whether typing a
 * six-digit number costs one parse or six.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorDebounceTest {

    private val evaluations = AtomicInteger()
    private val editor = EditorState { source ->
        evaluations.incrementAndGet()
        Quantity(BigDecimal(source.length)).ok()
    }

    @Test
    fun `a burst of keystrokes costs one evaluation, not one each`() = runTest {
        val loop = launchLoop()

        repeat(6) { editor.apply(EditorCommand.InsertText("1")) }
        advanceTimeBy(DEBOUNCE + 1)

        assertEquals(1, evaluations.get())
        loop()
    }

    @Test
    fun `nothing is evaluated while the user is still typing`() = runTest {
        val loop = launchLoop()

        editor.apply(EditorCommand.InsertText("1"))
        advanceTimeBy(DEBOUNCE - 1)

        assertEquals(0, evaluations.get())
        loop()
    }

    @Test
    fun `typing again before the debounce elapses restarts it`() = runTest {
        val loop = launchLoop()

        editor.apply(EditorCommand.InsertText("1"))
        advanceTimeBy(DEBOUNCE - 1)
        editor.apply(EditorCommand.InsertText("2"))
        advanceTimeBy(DEBOUNCE - 1)

        assertEquals("the second keystroke should have reset the timer", 0, evaluations.get())

        advanceTimeBy(2)
        assertEquals(1, evaluations.get())
        loop()
    }

    @Test
    fun `the evaluation that lands is the one for the final text`() = runTest {
        val loop = launchLoop()

        editor.apply(EditorCommand.InsertText("123"))
        advanceUntilIdle()

        val result = editor.evaluation
        assertEquals(EditorEvaluation.Result(Quantity(BigDecimal(3))), result)
        loop()
    }

    /** Starts the loop and returns the function that stops it. */
    private fun TestScope.launchLoop(): () -> Unit {
        val job = launch { editor.evaluationLoop(DEBOUNCE) }
        return { job.cancel() }
    }

    private companion object {
        const val DEBOUNCE = 400L
    }
}
