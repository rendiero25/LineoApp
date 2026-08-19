plugins {
    alias(libs.plugins.lineo.android.application)
    alias(libs.plugins.lineo.android.compose)
    alias(libs.plugins.lineo.android.hilt)
}

android {
    namespace = "app.lineo"
    defaultConfig.applicationId = "app.lineo"
}

// The attribution list is generated from the shipped licence allowlist on every build, and
// registered as a source directory rather than committed. That is what keeps it honest: the
// allowlist is already the file the licence gate enforces, so the screen cannot list a
// library the APK does not contain, or miss one it does.
androidComponents {
    onVariants { variant ->
        val generate = tasks.register(
            "generate${variant.name.replaceFirstChar(Char::titlecase)}LicenseAttribution",
            app.lineo.gradle.GenerateLicenseAttributionTask::class.java,
        ) {
            allowlist.set(rootProject.layout.projectDirectory.file("config/licenses/allowed-dependencies.txt"))
        }
        variant.sources.java?.addGeneratedSourceDirectory(
            generate,
            app.lineo.gradle.GenerateLicenseAttributionTask::outputDirectory,
        )
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:registry"))
    implementation(project(":core:ui"))
    implementation(project(":feature:notepad"))
    implementation(project(":feature:scientific"))
    implementation(project(":feature:converter"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Declared rather than inherited: `LocalLifecycleOwner`, `collectAsStateWithLifecycle` and
    // `viewModelScope` are used directly here. Both are already on the release classpath and
    // already in the licence allowlist, so this adds no artifact and no licence decision.
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
