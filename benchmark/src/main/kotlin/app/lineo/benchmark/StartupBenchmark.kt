package app.lineo.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * How long Lineo takes to become usable (P1-10).
 *
 * `docs/ANDROID_STANDARDS.md` §3 sets the target at under 500 ms of cold start on an API 26
 * device, and names startup as a *distribution* problem rather than only a craft one: poor
 * vitals cost store placement.
 *
 * Two compilations, deliberately. [CompilationMode.None] is the worst case — a first run on
 * a device that has never seen the app — and the profile-guided one is what a user gets from
 * the Play install. The difference between them is the baseline profile earning its place.
 *
 * `timeToFullDisplay` is the number that matters, not `timeToInitialDisplay`: the first frame
 * is a notepad with nothing in it, and the app is usable when the document has been read.
 * `ReportDrawnWhen` in `NotepadRoute` is what marks that moment.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmark = MacrobenchmarkRule()

    @Test
    fun startupWithoutAProfile() = measure(CompilationMode.None())

    @Test
    fun startupWithABaselineProfile() =
        measure(CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require))

    private fun measure(compilation: CompilationMode) = benchmark.measureRepeated(
        packageName = LINEO,
        metrics = listOf(StartupTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.COLD,
        compilationMode = compilation,
    ) {
        pressHome()
        startActivityAndWait()
    }
}

/** The package under test. The benchmark installs and drives the real release build. */
internal const val LINEO = "app.lineo"

/**
 * Enough runs for the median to mean something, few enough to finish in CI.
 *
 * Macrobenchmark reports median and range; ten cold starts of a 1.6 MB app take about a
 * minute on a warm device.
 */
internal const val ITERATIONS = 10
