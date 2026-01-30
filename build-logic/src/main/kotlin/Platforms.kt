package com.shakster.gifkt.gradle

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.assign
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

fun KotlinMultiplatformExtension.configurePlatforms(
    javaVersion: String,
) {
    jvmToolchain(javaVersion.toInt())

    jvm {
        compilerOptions {
            this.jvmTarget = getJvmTarget(javaVersion)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()

    watchosX64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
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

fun KotlinMultiplatformAndroidLibraryTarget.configureAndroid(
    androidCompileVersion: String,
    androidMinVersion: String,
    javaVersion: String,
) {
    namespace = "com.shakster.gifkt"
    compileSdk = androidCompileVersion.toInt()
    minSdk = androidMinVersion.toInt()

    withJava()
    withHostTestBuilder {}.configure {}
    withDeviceTestBuilder {
        sourceSetTreeName = "test"
    }

    compilerOptions {
        jvmTarget = getJvmTarget(javaVersion)
    }
}

fun getJvmTarget(javaVersion: String): JvmTarget {
    val version = if (javaVersion.toInt() > 8) javaVersion else "1.$javaVersion"
    return JvmTarget.fromTarget(version)
}

fun KotlinSourceSet.registerChildSourceSets(vararg sourceSets: Any) {
    sourceSets.forEach {
        when (it) {
            is KotlinSourceSet -> it.dependsOn(this)

            is Provider<*> -> {
                when (val sourceSet = it.get()) {
                    is KotlinSourceSet -> sourceSet.dependsOn(this)

                    else -> throw IllegalArgumentException(
                        "Expected Provider<KotlinSourceSet>, got: Provider<${sourceSet::class.qualifiedName ?: "Any"}>"
                    )
                }
            }

            else -> throw IllegalArgumentException(
                "Expected KotlinSourceSet or Provider<KotlinSourceSet>, got: ${it::class.qualifiedName ?: "Any"}"
            )
        }
    }
}
