import java.util.Properties

plugins {
    alias(libs.plugins.lineo.android.application)
    alias(libs.plugins.lineo.android.compose)
    alias(libs.plugins.lineo.android.hilt)
    // Screens live here now — settings, history, licences — and P1-06 recorded that they
    // would go unsnapshotted until this was applied. Test-only, so nothing reaches the APK.
    alias(libs.plugins.paparazzi)
    // Consumes the profile `:benchmark` generates and ships it in the release build.
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "app.lineo"
    defaultConfig.applicationId = "app.lineo"
}

// Paparazzi 2.0 ships Java 21 bytecode, while the shared test toolchain is 17 (the version
// detekt is pinned to). The override is local to the modules that render snapshots, as it is
// in :core:ui and :feature:notepad, rather than a build-wide bump.
tasks.withType<Test>().configureEach {
    javaLauncher.set(
        javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) },
    )
}

// The four names `keystore.properties` must carry, in one place so the file and the
// environment-variable path cannot drift apart.
val SIGNING_KEYS = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")

// Signing (P1-11-0). The upload key lives outside the repo: a gitignored
// `keystore.properties` on a workstation, the LINEO_KEYSTORE_* environment variables on CI.
// `keystore.properties.template` documents both, and how to generate the key.
//
// **Applied only when credentials are actually present.** P1-10-0 has CI build the release
// APK on every push to hold it under the 12 MB ceiling, and that check has no business
// holding a signing key — so with no credentials the release build still builds, unsigned.
// An unsigned APK is caught once, at upload; a CI job that cannot build is caught by
// everybody, every day.
val releaseSigning: Map<String, String>? = run {
    val properties = rootProject.file("keystore.properties")
    val credentials = if (properties.exists()) {
        val loaded = Properties().apply { properties.inputStream().use(::load) }
        SIGNING_KEYS.associateWith { loaded.getProperty(it).orEmpty() }
    } else {
        mapOf(
            "storeFile" to System.getenv("LINEO_KEYSTORE_PATH").orEmpty(),
            "storePassword" to System.getenv("LINEO_KEYSTORE_PASSWORD").orEmpty(),
            "keyAlias" to System.getenv("LINEO_KEY_ALIAS").orEmpty(),
            "keyPassword" to System.getenv("LINEO_KEY_PASSWORD").orEmpty(),
        )
    }
    // All four or none. Three of four is a typo, and a build that filled the fourth from a
    // default would produce an APK signed by a key nobody chose.
    credentials.takeIf { values -> values.values.none(String::isEmpty) }
}

android {
    if (releaseSigning != null) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(releaseSigning.getValue("storeFile"))
                storePassword = releaseSigning.getValue("storePassword")
                keyAlias = releaseSigning.getValue("keyAlias")
                keyPassword = releaseSigning.getValue("keyPassword")
            }
        }
        buildTypes.named("release") { signingConfig = signingConfigs.getByName("release") }
    }
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
    // Installs the baseline profile on first run, which is the whole point of having one.
    // The only part of the benchmark work that reaches the APK (P1-10).
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":benchmark"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
