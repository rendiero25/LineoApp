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

    testImplementation(libs.junit)
}
