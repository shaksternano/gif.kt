package com.shakster.gifkt.gradle

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.assign
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun KotlinMultiplatformExtension.configurePlatforms(javaVersion: String) {
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
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosX64()
    watchosSimulatorArm64()
    macosX64()
    macosArm64()
    linuxX64()
    linuxArm64()
    mingwX64()

    js {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    applyDefaultHierarchyTemplate()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

fun LibraryExtension.configureAndroid(
    androidCompileVersion: String,
    androidMinVersion: String,
    javaVersion: String,
) {
    namespace = "com.shakster.gifkt"
    compileSdk = androidCompileVersion.toInt()
    defaultConfig {
        minSdk = androidMinVersion.toInt()
    }
    compileOptions {
        val javaVersion = JavaVersion.toVersion(javaVersion)
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

private fun getJvmTarget(javaVersion: String): JvmTarget {
    val version = if (javaVersion.toInt() > 8) javaVersion else "1.$javaVersion"
    return JvmTarget.fromTarget(version)
}
