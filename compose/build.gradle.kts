import com.shakster.gifkt.gradle.*
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("com.shakster.gifkt.gradle.gifkt-plugin")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.maven.publish)
    alias(libs.plugins.dokka)
}

val artifactId = "gifkt-compose"
base.archivesName = artifactId

val javaVersion = "11"
val androidCompileVersion = libs.versions.android.compileSdk.get()
val androidMinVersion = libs.versions.android.minSdk.get()

kotlin {
    jvmToolchain(javaVersion.toInt())

    jvm {
        compilerOptions {
            this.jvmTarget = getJvmTarget(javaVersion)
        }
    }

    android {
        configureAndroid(
            namespace = "com.shakster.gifkt.compose",
            androidCompileVersion = androidCompileVersion,
            androidMinVersion = androidMinVersion,
            javaVersion = javaVersion,
        )
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

        val parallelMain by creating {
            dependsOn(commonMain.get())
        }
        parallelMain.registerChildSourceSets(jvmMain, androidMain, nativeMain)

        commonMain.dependencies {
            api(project(":core"))
            api(libs.compose.runtime)
            api(libs.compose.ui)
        }
    }
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
