plugins {
    alias(libs.plugins.lineo.android.library)
    alias(libs.plugins.lineo.android.hilt)
}

android {
    namespace = "app.lineo.billing"
}

dependencies {
    implementation(libs.androidx.billing.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
