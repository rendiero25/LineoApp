package app.lineo.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What typing costs in frames (P1-10).
 *
 * The target of `docs/ANDROID_STANDARDS.md` §3 is no dropped frames while typing, and typing
 * is where a notepad calculator does its work: every keystroke re-evaluates the lines the
 * change reached and re-renders them. `FrameTimingMetric` reports the frame durations, so a
 * regression shows up as a percentile rather than as a feeling.
 *
 * The journey is the document of P1-02's own benchmark, typed on a device: a definition, a
 * line that reads it, and a line that reads that. Each keystroke is a real key press through
 * the keypad, which is the path a user takes and the path a unit test cannot measure.
 */
@RunWith(AndroidJUnit4::class)
class TypingBenchmark {

    @get:Rule
    val benchmark = MacrobenchmarkRule()

    @Test
    fun typingADocument() = benchmark.measureRepeated(
        packageName = LINEO,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        // Warm rather than cold: this measures typing, and a cold start in the middle of it
        // would put the cost of reading the document into the frame timings.
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.DEFAULT,
        setupBlock = { startActivityAndWait() },
    ) {
        typeDocument()
    }
}
