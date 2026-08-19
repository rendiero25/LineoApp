plugins {
    alias(libs.plugins.lineo.android.library)
    alias(libs.plugins.lineo.android.compose)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "app.lineo.scientific"
}

// Paparazzi 2.0 ships Java 21 bytecode, while the shared test toolchain is 17 (the version
// detekt is pinned to). The override is local to the modules that render snapshots, as it is
// in :core:ui and :feature:notepad, rather than a build-wide bump.
tasks.withType<Test>().configureEach {
    javaLauncher.set(
        javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) },
    )
}

dependencies {
    // The module contract, and the CalcFunction shape its functions are written in.
    api(project(":core:registry"))
    // Quantity, CalcResult and the built-ins these functions are composed from.
    api(project(":core:engine"))
    // The editor, the keypad and the layout the screen is assembled from. Nothing here
    // draws a key of its own; only the rows are this module's.
    api(project(":core:ui"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
