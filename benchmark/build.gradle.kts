plugins {
    // Versionless: AGP is already on the build classpath through `build-logic`, and asking
    // for a version here fails outright ("already on the classpath with an unknown version").
    id("com.android.test")
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "app.lineo.benchmark"
    compileSdk = 37

    defaultConfig {
        // 28 rather than the app's 26: Macrobenchmark reads the system traces it measures
        // from, and those need Android 9. The *app* still ships to 26 — this module never
        // does, so it is only the measuring instrument that needs a newer device.
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // What is measured is what a user installs. A debug build is slower in ways nobody ships,
    // so a number taken from one would be a number about the wrong app.
    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

baselineProfile {
    // The profile is generated on whatever device is attached and lands in `:app`. No managed
    // device is declared: adding one downloads a system image on first use, which is a
    // decision about CI cost rather than about the profile.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.junit)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
