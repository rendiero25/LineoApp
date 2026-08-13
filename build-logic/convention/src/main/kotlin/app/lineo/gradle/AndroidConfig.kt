package app.lineo.gradle

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Platform targets, locked in `docs/ARCHITECTURE.md` §10. Changing [MIN_SDK] requires a
 * human decision; it is a floor, not a default.
 */
internal const val MIN_SDK = 26
internal const val TARGET_SDK = 37
internal const val COMPILE_SDK = 37
internal const val ANDROID_TEST_RUNNER = "androidx.test.runner.AndroidJUnitRunner"

internal val JAVA_VERSION = JavaVersion.VERSION_17
internal val JVM_TARGET = JvmTarget.JVM_17
internal const val JVM_TOOLCHAIN = 17

/** Configuration shared by every Android module, application or library. */
internal fun Project.configureAndroid(extension: CommonExtension) {
    extension.compileSdk = COMPILE_SDK

    extension.compileOptions.sourceCompatibility = JAVA_VERSION
    extension.compileOptions.targetCompatibility = JAVA_VERSION

    extension.lint.warningsAsErrors = true
    extension.lint.abortOnError = true
    extension.lint.checkDependencies = true
    // `docs/ARCHITECTURE.md` §10: an unguarded call above API 26 must fail the build.
    extension.lint.error.addAll(listOf("NewApi", "InlinedApi"))
    // Version freshness is a human decision (`AGENTS.md` §7) and the check needs the
    // network, which would make the build non-deterministic.
    extension.lint.disable.add("GradleDependency")

    configureKotlinAndroid()
    configureTests()
    configureDetekt()
    configureLicenseCheck()
}

/** JVM target for Android modules. Kotlin is applied by AGP's built-in Kotlin support. */
internal fun Project.configureKotlinAndroid() {
    extensions.configure(KotlinAndroidProjectExtension::class.java) {
        compilerOptions {
            jvmTarget.set(JVM_TARGET)
            allWarningsAsErrors.set(true)
        }
    }
}

/** JVM target for pure Kotlin modules such as `:core:engine`. */
internal fun Project.configureKotlinJvm() {
    extensions.configure(KotlinJvmProjectExtension::class.java) {
        jvmToolchain(JVM_TOOLCHAIN)
        compilerOptions {
            jvmTarget.set(JVM_TARGET)
            allWarningsAsErrors.set(true)
        }
    }
}

/**
 * Android unit tests stay on JUnit 4 — androidx.test and Robolectric are built on it.
 * Pure Kotlin modules use JUnit 5 via [configureJvmTests].
 */
internal fun Project.configureTests() {
    val launcher = extensions.getByType(JavaToolchainService::class.java)
        .launcherFor { languageVersion.set(JavaLanguageVersion.of(JVM_TOOLCHAIN)) }
    tasks.withType<Test>().configureEach {
        javaLauncher.set(launcher)
        testLogging {
            events("failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}

internal fun Project.configureJvmTests() {
    configureTests()
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
