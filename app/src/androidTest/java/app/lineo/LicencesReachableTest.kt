package app.lineo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.lineo.licenses.attributedDependencies
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The attribution obligation, asserted against a built app rather than against the allowlist
 * (P1-11-3).
 *
 * `AttributionTest` already proves the *list* is right: it is generated from the enforced
 * licence allowlist, so it cannot name a library the APK lacks or miss one it has. What that
 * test cannot see is everything between the list and a user's thumb — whether the screen is
 * reachable at all, and whether the licence text survives the build.
 *
 * The second is the real hazard. `docs/ANDROID_STANDARDS.md` §3 has the release build running
 * R8 in full mode *with resource shrinking*, and `res/raw/license_apache_2_0.txt` is reached
 * through a map of SPDX identifiers to resource ids rather than by a literal `R.raw`
 * reference in the calling code. A shrinker that decided nothing referenced it would leave a
 * screen that names every licence and can open none of them — which satisfies Apache-2.0 §4
 * no better than saying nothing.
 *
 * No Hilt test rule: `LineoApplication` is `@HiltAndroidApp`, so the real activity injects
 * from the real graph. `HiltAndroidRule` exists to *replace* bindings, which this test has no
 * reason to do — and `hilt-android-testing` would be a dependency, which `AGENTS.md` §7
 * reserves for a human.
 *
 * Runs on the debug build, so it proves the path and not the shrinker. The release build is
 * verified by hand against a signed APK, and recorded in the TASKS.md log.
 */
@RunWith(AndroidJUnit4::class)
class LicencesReachableTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** Overflow, then Settings, then the licences row — the way a user gets there. */
    private fun openLicences() {
        compose.onNodeWithContentDescription(string(R.string.menu_more_options)).performClick()
        compose.onNodeWithText(string(R.string.settings_title)).performClick()
        compose.onNodeWithText(string(R.string.settings_licences)).performClick()
    }

    private fun string(id: Int): String = compose.activity.getString(id)

    @Test
    fun theLicencesScreenIsReachableFromTheNotepad() {
        openLicences()

        // Not "some list appeared": the first coordinate the generator emitted, so a screen
        // that rendered an empty list fails here rather than passing quietly.
        compose.onNodeWithText(attributedDependencies.first().module).assertIsDisplayed()
    }

    @Test
    fun everyShippedDependencyIsNamed() {
        assertTrue("the generated attribution list is empty", attributedDependencies.isNotEmpty())
        openLicences()

        // The list is lazy, so an entry that is never scrolled to is never composed. Each one
        // is scrolled into view in turn — the only way to ask a LazyColumn whether it really
        // holds what it claims to.
        attributedDependencies.forEach { dependency ->
            compose.onNode(hasScrollAction()).performScrollToNode(hasText(dependency.module))
            compose.onNodeWithText(dependency.module).assertIsDisplayed()
        }
    }

    @Test
    fun theLicenceTextTravelsWithTheBinary() {
        openLicences()

        compose.onNodeWithText(attributedDependencies.first().module).performClick()

        // A phrase from `res/raw/license_apache_2_0.txt` itself. Asserting the row expanded
        // would pass against a bundled text that had been shrunk away, because the screen
        // renders `licences_missing_text` in its place and stays perfectly usable.
        compose.onNodeWithText("Apache License", substring = true).assertIsDisplayed()
    }
}
