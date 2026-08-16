import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Hilt for an Android module. Apply alongside `lineo.android.library` or
 * `lineo.android.application`; it never applies an Android plugin itself.
 *
 * Modules are contributed to a `Set<CalculatorModule>` by multibinding
 * (`docs/ARCHITECTURE.md` §4), so every module that binds one needs the annotation
 * processor, not only `:app`.
 */
class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("com.google.dagger.hilt.android")

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            dependencies {
                add("implementation", libs.findLibrary("hilt-android").get())
                add("ksp", libs.findLibrary("hilt-compiler").get())
            }
        }
    }
}
