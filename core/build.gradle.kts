import com.shakster.gifkt.gradle.*

plugins {
    id("com.shakster.gifkt.gradle.gifkt-plugin")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.maven.publish)
    alias(libs.plugins.dokka)
}

val artifactId = "gifkt"
base.archivesName = artifactId

val javaVersion = libs.versions.java.get()
val androidCompileVersion = libs.versions.android.compileSdk.get()
val androidMinVersion = libs.versions.android.minSdk.get()

kotlin {
    configurePlatforms(
        javaVersion = javaVersion,
    )

    android {
        configureAndroid(
            androidCompileVersion = androidCompileVersion,
            androidMinVersion = androidMinVersion,
            javaVersion = javaVersion,
        )
    }

    sourceSets {
        val nonJvmMain by creating {
            dependsOn(commonMain.get())
        }
        nonJvmMain.registerChildSourceSets(nativeMain, jsMain, wasmJsMain, wasmWasiMain)

        val androidAndJvmMain by creating {
            dependsOn(commonMain.get())
        }
        androidAndJvmMain.registerChildSourceSets(androidMain, jvmMain)

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

        androidAndJvmMain.dependencies {
            implementation(libs.androidx.collection)
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
    }
}

mavenPublishing {
    configurePublishing(
        project = project,
        artifactId = artifactId,
        pomName = rootProject.name,
        pomDescription = "A Kotlin GIF encoding and decoding library",
    )
}

dokka {
    configureDokka(
        project = project,
        moduleName = "gifkt",
        javaVersion = javaVersion,
    )
}
