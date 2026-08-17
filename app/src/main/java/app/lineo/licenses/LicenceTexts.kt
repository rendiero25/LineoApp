package app.lineo.licenses

import app.lineo.R

/**
 * The full licence texts the APK carries, keyed by SPDX identifier.
 *
 * Apache-2.0 §4 requires a copy of the licence to travel with the binary, so naming a licence
 * on the attribution screen is not enough — the text has to be there to open. Every family in
 * [attributedDependencies] must appear here, which `AttributionTest` enforces: adding a
 * dependency under a family with no bundled text fails the build rather than shipping a
 * screen that points at nothing.
 */
object LicenceTexts {

    private val byFamily: Map<String, Int> = mapOf(
        "Apache-2.0" to R.raw.license_apache_2_0,
    )

    /** Families with a text bundled in `res/raw`. */
    val families: Set<String> get() = byFamily.keys

    /**
     * The raw resource holding the full text of [family], or null when none is bundled.
     *
     * Null is a programming error rather than a state to render — the test above rules it
     * out — so a caller may treat it as "this family should not have reached me".
     */
    fun resourceFor(family: String): Int? = byFamily[family]
}
