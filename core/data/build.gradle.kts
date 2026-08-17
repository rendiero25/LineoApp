plugins {
    alias(libs.plugins.lineo.android.library)
    alias(libs.plugins.lineo.android.hilt)
}

android {
    namespace = "app.lineo.data"
    testOptions.unitTests.isIncludeAndroidResources = true
}

// Room's exported schemas are read as assets by the migration test, which runs on
// Robolectric. Configured through the typed AGP interface: the generated `android { }`
// accessor still resolves `sourceSets` to the removed legacy type.
configure<com.android.build.api.dsl.LibraryExtension> {
    sourceSets.getByName("test").assets.directories.add(layout.projectDirectory.dir("schemas").asFile.path)
}

ksp {
    arg("room.schemaLocation", layout.projectDirectory.dir("schemas").asFile.path)
    arg("room.generateKotlin", "true")
}

dependencies {
    api(project(":core:engine"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
