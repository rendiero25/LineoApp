plugins {
    alias(libs.plugins.lineo.android.library)
    alias(libs.plugins.lineo.android.compose)
}

android {
    namespace = "app.lineo.registry"
}

dependencies {
    api(project(":core:engine"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.annotation)
    testImplementation(libs.junit)
}
