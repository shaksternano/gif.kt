import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.net.URI

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.maven.publish)
    alias(libs.plugins.dokka)
}

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

        nativeMain.dependencies {
            implementation(libs.androidx.collection)
        }

        webMain.dependencies {
            implementation(libs.androidx.collection)
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
            implementation(libs.androidx.collection)
        }

        val jsAndWasmMain by creating {
            dependsOn(commonMain.get())
        }
        jsAndWasmMain.registerChildSourceSets(jsMain, wasmJsMain, wasmWasiMain)

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
        url = "https://github.com/shaksternano/gif.kt"
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
            url = "https://github.com/shaksternano/gif.kt"
            connection = "scm:git:git://github.com/shaksternano/gif.kt.git"
            developerConnection = "scm:git:ssh://github.com/shaksternano/gif.kt.git"
        }
    }
}

tasks {
    withType<DokkaTask>().configureEach {
        dokkaSourceSets.configureEach {
            moduleName = "gif.kt"
            includes.from("README.md")
            jdkVersion = 8

            sourceLink {
                localDirectory = file("src")
                remoteUrl = URI("https://github.com/shaksternano/gif.kt/tree/main/core/src").toURL()
                remoteLineSuffix = "#L"
            }

            externalDocumentationLink {
                url = URI("https://kotlinlang.org/api/kotlinx.coroutines/").toURL()
            }

            externalDocumentationLink {
                url = URI("https://kotlinlang.org/api/kotlinx-io/").toURL()
            }
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
