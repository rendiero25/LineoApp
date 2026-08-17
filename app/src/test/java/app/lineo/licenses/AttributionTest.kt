package app.lineo.licenses

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the generated attribution list.
 *
 * The generator reads the same file the licence gate enforces, so the list cannot drift from
 * what ships. What it cannot check is whether the app carries the licence *texts* those
 * entries refer to — Apache-2.0 §4 wants a copy of the licence in the distribution, and a
 * screen naming a licence the APK does not contain satisfies nobody.
 */
class AttributionTest {

    @Test
    fun `the attribution list is not empty`() {
        // A blank screen is the failure mode nobody notices until an audit.
        assertTrue(attributedDependencies.isNotEmpty())
    }

    /**
     * Adding a dependency under a family with no bundled text fails here, which is the
     * intended moment to add the text — not the moment someone audits the store listing.
     */
    @Test
    fun `every attributed licence has its text bundled in the app`() {
        val missing = attributedDependencies
            .map { it.licence.family() }
            .distinct()
            .filterNot { it in LicenceTexts.families }

        assertEquals("no licence text bundled for: $missing", emptyList<String>(), missing)
    }

    @Test
    fun `no dependency is attributed twice`() {
        val duplicates = attributedDependencies
            .groupingBy { it.module }
            .eachCount()
            .filterValues { it > 1 }
            .keys

        assertEquals(emptySet<String>(), duplicates)
    }

    @Test
    fun `the list is sorted, so a diff shows what changed and not where it moved`() {
        assertEquals(attributedDependencies.map { it.module }.sorted(), attributedDependencies.map { it.module })
    }

    /**
     * The SPDX identifier at the head of an allowlist entry, without whatever follows it.
     *
     * An entry reads `Apache-2.0 (its POM declares…)` or `MIT Copyright (c) 2019 Author`, so
     * cutting at the first bracket is not enough — it leaves `MIT Copyright`. The identifier
     * is the leading run of identifier characters and nothing else.
     */
    private fun String.family(): String = SPDX_IDENTIFIER.find(this)?.value.orEmpty()

    private companion object {
        val SPDX_IDENTIFIER = Regex("^[A-Za-z0-9.-]+")
    }
}
