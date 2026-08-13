import app.lineo.gradle.ANDROID_TEST_RUNNER
import app.lineo.gradle.MIN_SDK
import app.lineo.gradle.configureAndroid
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                configureAndroid(this)
                defaultConfig {
                    minSdk = MIN_SDK
                    testInstrumentationRunner = ANDROID_TEST_RUNNER
                }
            }
        }
    }
}
