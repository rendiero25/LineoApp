import app.lineo.gradle.configureDetekt
import app.lineo.gradle.configureJvmTests
import app.lineo.gradle.configureKotlinJvm
import app.lineo.gradle.configureLicenseCheck
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Pure Kotlin/JVM module. `:core:engine` uses this — it must never see `android.*`.
 * See `docs/ARCHITECTURE.md` §10.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("java-library")

            configureKotlinJvm()
            configureJvmTests()
            configureDetekt()
            configureLicenseCheck()

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            dependencies.add(
                "testImplementation",
                dependencies.platform(libs.findLibrary("junit-bom").get()),
            )
            dependencies.add("testImplementation", libs.findLibrary("junit-jupiter").get())
            dependencies.add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
        }
    }
}
