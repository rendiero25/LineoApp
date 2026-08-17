plugins {
    alias(libs.plugins.lineo.android.library)
    alias(libs.plugins.lineo.android.compose)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "app.lineo.ui"
}

// Paparazzi 2.0 ships Java 21 bytecode, while the shared test toolchain is 17 (the version
// detekt is pinned to). Only this module's tests need the newer runtime, so the override is
// local rather than a build-wide bump.
tasks.withType<Test>().configureEach {
    javaLauncher.set(
        javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) },
    )
}

dependencies {
    implementation(libs.androidx.annotation)

    testImplementation(libs.junit)
}
