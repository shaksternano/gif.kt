plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.gradle.shadow)
    alias(libs.plugins.graalvm.native)
}

base.archivesName = "gifkt-cli"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core"))
    implementation(libs.clikt)
    implementation(libs.javacv) {
        excludeJavaCpp(
            "artoolkitplus",
            "flandmark",
            "flycapture",
            "leptonica",
            "libdc1394",
            "libfreenect",
            "libfreenect2",
            "librealsense",
            "librealsense2",
            "openblas",
            "opencv",
            "tesseract",
            "videoinput",
        )
    }
}

val mainClassFullName = "${project.group}.gifkt.cli.MainKt"

application {
    mainClass = mainClassFullName
    tasks.run.get().workingDir = rootProject.projectDir
}

graalvmNative {
    agent {
        defaultMode = "standard"
    }

    binaries {
        named("main") {
            imageName = "gifkt"
        }
    }

    metadataRepository {
        enabled = true
    }
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier = ""
        mergeServiceFiles()
        manifest {
            attributes("Main-Class" to mainClassFullName)
        }
        dependsOn(distTar, distZip)
    }

    build {
        dependsOn(shadowJar)
    }

    generateResourcesConfigFile {
        dependsOn(shadowJar)
    }
}

/**
 * Exclude unused native libraries in order to reduce the JAR size.
 */
fun ModuleDependency.excludeJavaCpp(vararg modules: String) = modules.forEach {
    exclude(group = "org.bytedeco", module = it)
    exclude(group = "org.bytedeco", module = "$it-platform")
}
