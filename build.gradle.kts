plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.gradle.shadow) apply false
    alias(libs.plugins.graalvm.native) apply false
}

allprojects {
    group = "com.shakster"
    version = "0.3.2"
}

dependencies {
    dokka(project(":core"))
    dokka(project(":compose"))
}
