plugins {
    alias(libs.plugins.lineo.android.library)
}

android {
    namespace = "app.lineo.notepad"
}

dependencies {
    // The document model speaks in LineId and hands canonical source to the engine, so the
    // engine is part of this module's own API rather than an implementation detail.
    api(project(":core:engine"))
    // For the line edit and the EditorCommand contract it applies — the same code path the
    // single-line editor uses, so `±` and backspace cannot drift between the two surfaces.
    api(project(":core:ui"))
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
