plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.gradle.plugin)
    implementation(libs.vanniktech.maven.publish.gradle.plugin)
    implementation(libs.dokka.gradle.plugin)
}

gradlePlugin {
    plugins {
        create("gifkt-plugin") {
            id = "com.shakster.gifkt.gradle.gifkt-plugin"
            implementationClass = "com.shakster.gifkt.gradle.GifKtPlugin"
        }
    }
}
