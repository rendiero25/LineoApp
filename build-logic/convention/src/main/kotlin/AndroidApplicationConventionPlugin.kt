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
            }
        }
    }
}
