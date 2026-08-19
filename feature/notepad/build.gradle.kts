plugins {
    alias(libs.plugins.lineo.android.library)
    alias(libs.plugins.lineo.android.compose)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "app.lineo.notepad"
}

// Paparazzi 2.0 ships Java 21 bytecode, while the shared test toolchain is 17 (the version
// detekt is pinned to). The override is local to the modules that render snapshots, as it is
// in :core:ui, rather than a build-wide bump.
tasks.withType<Test>().configureEach {
    javaLauncher.set(
        javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) },
    )
}

dependencies {
    // The document model speaks in LineId and hands canonical source to the engine, so the
    // engine is part of this module's own API rather than an implementation detail.
    api(project(":core:engine"))
    // For the line edit and the EditorCommand contract it applies — the same code path the
    // single-line editor uses, so `±` and backspace cannot drift between the two surfaces.
    api(project(":core:ui"))
    // The documents and lines rows of `docs/ARCHITECTURE.md` §6. Only the repository contract
    // is used; Room stays behind it, and no entity crosses this boundary.
    implementation(project(":core:data"))
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
