import app.lineo.gradle.configureDetekt
import app.lineo.gradle.configureJvmTests
import app.lineo.gradle.configureKotlinJvm
import app.lineo.gradle.configureLicenseCheck
import org.gradle.api.Plugin
import org.gradle.api.Project

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
        }
    }
}
