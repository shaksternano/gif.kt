import com.shakster.gifkt.gradle.configureAndroid
import com.shakster.gifkt.gradle.configureDokka
import com.shakster.gifkt.gradle.configurePublishing
import com.shakster.gifkt.gradle.getJvmTarget
import com.shakster.gifkt.gradle.registerChildSourceSets
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("com.shakster.gifkt.gradle.gifkt-plugin")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.maven.publish)
    alias(libs.plugins.dokka)
}

val artifactId = "gifkt-compose"
base.archivesName = artifactId

val javaVersion = libs.versions.java.get()
val androidCompileVersion = libs.versions.android.compileSdk.get()
val androidMinVersion = libs.versions.android.minSdk.get()

kotlin {
    jvmToolchain(javaVersion.toInt())

    val jvmTarget = getJvmTarget(javaVersion)

    jvm {
        compilerOptions {
            this.jvmTarget = jvmTarget
        }
    }

    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            this.jvmTarget = jvmTarget
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    macosX64()
    macosArm64()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    applyDefaultHierarchyTemplate()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        val nonAndroidMain by creating {
            dependsOn(commonMain.get())
        }
        nonAndroidMain.registerChildSourceSets(jvmMain, nativeMain, webMain)

        commonMain.dependencies {
            api(project(":core"))
            api(libs.compose.ui.graphics)
        }
    }
}

android {
    configureAndroid(
        androidCompileVersion = androidCompileVersion,
        androidMinVersion = androidMinVersion,
        javaVersion = javaVersion,
    )
}

mavenPublishing {
    configurePublishing(
        project = project,
        artifactId = artifactId,
        pomName = "${rootProject.name} compose",
        pomDescription = "Compose Multiplatform integration for gif.kt"
    )
}

dokka {
    configureDokka(
        project = project,
        moduleName = "gifkt-compose",
        javaVersion = javaVersion,
    )
}
