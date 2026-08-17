package app.lineo.licenses

/**
 * One row of the open-source attribution screen.
 *
 * The list itself is generated at build time from `config/licenses/allowed-dependencies.txt`
 * by `:app:generateLicenseAttribution`, so it always matches what the APK actually contains —
 * see [attributedDependencies].
 *
 * @param module the Maven coordinate, `group:name`, without a version. Users are being told
 *   what is inside the app, not which build of it.
 * @param licence the licence as recorded in the allowlist, including any note.
 */
data class LicensedDependency(val module: String, val licence: String)

/**
 * Every third-party library that ships inside Lineo, sorted by coordinate.
 *
 * Backed by generated code rather than an asset so that reading it needs no `Context`:
 * `docs/ANDROID_STANDARDS.md` §1 keeps those out of every layer above the data one, and the
 * settings screen has no business opening a file to render a list of names.
 */
val attributedDependencies: List<LicensedDependency> get() = GENERATED_ATTRIBUTION
