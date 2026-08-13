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
            }
        }
    }
}
