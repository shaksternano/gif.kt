plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.gradleShadow) apply false
    alias(libs.plugins.graalvmNative) apply false
    alias(libs.plugins.dokka) apply false
}

allprojects {
    group = "com.shakster"
    version = "0.1.2"
}
