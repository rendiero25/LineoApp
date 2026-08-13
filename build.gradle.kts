// Top-level build file. Real configuration lives in build-logic; this only puts the
// underlying plugins on the classpath so the convention plugins can apply them by id.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
