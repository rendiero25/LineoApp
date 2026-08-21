package app.lineo.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Writes the baseline profile the release build ships with (P1-10).
 *
 * A baseline profile is a list of the classes and methods a *typical first run* touches, so
 * ART can compile them ahead of time instead of interpreting them on the way to the first
 * frame. It is generated from the same journey the benchmarks measure — start the app, type
 * two lines — because a profile taken from a different journey optimises code the user's
 * first run never reaches.
 *
 * Regenerate with `./gradlew :app:generateReleaseBaselineProfile` on a rooted emulator or a
 * userdebug device; the output lands in `app/src/release/generated/baselineProfiles` and is
 * committed, so a build with no device attached still ships the profile.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startAndType() = rule.collect(
        packageName = LINEO,
        // The startup profile as well: it is the subset ART uses to lay out the dex file, and
        // it comes from the same run rather than from a second journey.
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        typeDocument()
    }
}
