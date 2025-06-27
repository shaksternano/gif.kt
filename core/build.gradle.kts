import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "com.shakster"
version = "0.1.0"

if (isRunningTask("publishAllPublicationsToMavenCentralRepository")) {
    version = "${version}-SNAPSHOT"
}

val artifactId = "gifkt"
base.archivesName = artifactId

kotlin {
    jvmToolchain(8)

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }

    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
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

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.io.core)
            implementation(libs.kotlinx.io.okio)
            implementation(libs.okio)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }

        wasmWasiMain.dependencies {
            implementation(libs.okio.wasifilesystem)
        }

        val nonJvmMain by creating {
            dependsOn(commonMain.get())
        }
        nonJvmMain.registerChildSourceSets(nativeMain, jsMain, wasmJsMain, wasmWasiMain)

        val androidAndJvmMain by creating {
            dependsOn(commonMain.get())
        }
        androidAndJvmMain.registerChildSourceSets(androidMain, jvmMain)
        androidAndJvmMain.dependencies {
            implementation(libs.eclipse.collections.api)
            runtimeOnly(libs.eclipse.collections)
        }

        val parallelMain by creating {
            dependsOn(commonMain.get())
        }
        parallelMain.registerChildSourceSets(androidAndJvmMain, nativeMain)

        val fileSystemMain by creating {
            dependsOn(commonMain.get())
        }
        fileSystemMain.registerChildSourceSets(androidAndJvmMain, nativeMain, wasmWasiMain)
    }

    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }
}

android {
    namespace = "com.shakster.gifkt"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

mavenPublishing {
    publishToMavenCentral()

    if (!isRunningTask("publishToMavenLocal")) {
        signAllPublications()
    }

    coordinates(group.toString(), artifactId, version.toString())

    pom {
        name = rootProject.name
        description = "A Kotlin GIF encoding and decoding library"
        inceptionYear = "2024"
        url = "https://github.com/shaksternano/gifkt"
        licenses {
            license {
                name = "MIT"
                url = "https://opensource.org/license/mit"
            }
        }
        developers {
            developer {
                id = "shaksternano"
                name = "ShaksterNano"
                url = "https://shakster.com"
            }
        }
        scm {
            url = "https://github.com/shaksternano/gifkt"
            connection = "scm:git:git://github.com/shaksternano/gifkt.git"
            developerConnection = "scm:git:ssh://github.com/shaksternano/gifkt.git"
        }
    }
}

fun isRunningTask(taskName: String): Boolean {
    return gradle.startParameter.taskNames.any {
        it.equals(taskName, ignoreCase = true)
    }
}

fun KotlinSourceSet.registerChildSourceSets(vararg sourceSets: Any) {
    sourceSets.forEach {
        if (it is KotlinSourceSet) {
            it.dependsOn(this)
        } else if (it is Provider<*>) {
            val sourceSet = it.get()
            if (sourceSet is KotlinSourceSet) {
                sourceSet.dependsOn(this)
            } else {
                throw IllegalArgumentException(
                    "Expected Provider<KotlinSourceSet>, got: Provider<${sourceSet::class.qualifiedName ?: "Any"}>"
                )
            }
        } else {
            throw IllegalArgumentException(
                "Expected KotlinSourceSet or Provider<KotlinSourceSet>, got: ${it::class.qualifiedName ?: "Any"}"
            )
        }
    }
}
