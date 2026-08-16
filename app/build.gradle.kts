plugins {
    alias(libs.plugins.lineo.android.application)
    alias(libs.plugins.lineo.android.compose)
    alias(libs.plugins.lineo.android.hilt)
}

android {
    namespace = "app.lineo"
    defaultConfig.applicationId = "app.lineo"
}

dependencies {
    implementation(project(":core:registry"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
