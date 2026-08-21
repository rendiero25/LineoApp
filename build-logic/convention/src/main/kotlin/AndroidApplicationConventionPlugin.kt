import app.lineo.gradle.ANDROID_TEST_RUNNER
import app.lineo.gradle.MIN_SDK
import app.lineo.gradle.TARGET_SDK
import app.lineo.gradle.configureAndroid
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                configureAndroid(this)
                defaultConfig {
                    minSdk = MIN_SDK
                    targetSdk = TARGET_SDK
                    versionCode = 1
                    versionName = "1.0"
                    testInstrumentationRunner = ANDROID_TEST_RUNNER
                }
                buildTypes.named("debug") {
                    // Generates the `en-XA` and `ar-XB` resources P1-09 asks for. They are
                    // built by aapt2 from the base strings, so they cost nothing to maintain
                    // and they are the only way to see truncation and bidi before a
                    // translation exists. Debug only: nothing pseudo reaches a release APK.
                    isPseudoLocalesEnabled = true
                }
                buildTypes.named("release") {
                    // R8, in the full mode AGP 8 made the default: what ships is only the
                    // code that is reachable, which is what `docs/ANDROID_STANDARDS.md` §3
                    // means by an APK under 12 MB. Shrinking resources with it, since a
                    // drawable nothing draws costs the same as one that ships.
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                }
            }
        }
    }
}
