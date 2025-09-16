plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
    alias(libs.plugins.gradle.shadow) apply false
    alias(libs.plugins.graalvm.native) apply false
    alias(libs.plugins.dokka) apply false
}

allprojects {
    group = "com.shakster"
    version = "0.1.2"
}
