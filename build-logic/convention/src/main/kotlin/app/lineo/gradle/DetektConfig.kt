package app.lineo.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.process.CommandLineArgumentProvider

/**
 * detekt with the ktlint formatting ruleset, giving a single static-analysis entry
 * point: `./gradlew detekt`.
 *
 * detekt runs through its CLI in a forked JVM rather than through its Gradle plugin.
 * The plugin executes in-process, and the Kotlin compiler bundled in detekt 1.23.x
 * cannot parse a JDK 25 version string, which is what the Gradle daemon runs on here.
 * Forking onto the project's Java 17 toolchain sidesteps that without pinning the whole
 * build to an older JVM.
 */
internal fun Project.configureDetekt() {
    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

    val cli = configurations.create("detektCli") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
    val plugins = configurations.create("detektRuleSets") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
    dependencies.add(cli.name, libs.findLibrary("detekt-cli").get())
    dependencies.add(plugins.name, libs.findLibrary("detekt-formatting").get())

    val launcher = extensions.getByType(JavaToolchainService::class.java)
        .launcherFor { languageVersion.set(JavaLanguageVersion.of(JVM_TOOLCHAIN)) }

    val cliFiles: FileCollection = cli.incoming.files
    val ruleSetFiles: FileCollection = plugins.incoming.files

    val configFile = rootProject.layout.projectDirectory.file("config/detekt/detekt.yml")
    val sourceDir = layout.projectDirectory.dir("src")
    val reportDir = layout.buildDirectory.dir("reports/detekt")

    val configPath = configFile.asFile.absolutePath
    val sourcePath = sourceDir.asFile.absolutePath
    val basePath = rootProject.projectDir.absolutePath
    val xmlReport = reportDir.get().file("detekt.xml").asFile.absolutePath
    val htmlReport = reportDir.get().file("detekt.html").asFile.absolutePath

    val detekt = tasks.register<JavaExec>("detekt") {
        group = "verification"
        description = "Runs detekt, including the ktlint formatting rules."

        javaLauncher.set(launcher)
        classpath = cliFiles
        mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")

        inputs.dir(sourceDir).withPropertyName("sources").skipWhenEmpty()
        inputs.file(configFile).withPropertyName("config")
        inputs.files(ruleSetFiles).withPropertyName("ruleSets")
        outputs.dir(reportDir).withPropertyName("reports")

        argumentProviders.add(
            CommandLineArgumentProvider {
                listOf(
                    "--input", sourcePath,
                    "--config", configPath,
                    "--build-upon-default-config",
                    "--base-path", basePath,
                    "--parallel",
                    "--plugins", ruleSetFiles.asPath,
                    "--report", "xml:$xmlReport",
                    "--report", "html:$htmlReport",
                )
            },
        )
    }

    tasks.named("check").configure { dependsOn(detekt) }
}
