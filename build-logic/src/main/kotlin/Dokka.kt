package com.shakster.gifkt.gradle

import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.jetbrains.dokka.gradle.DokkaExtension

fun DokkaExtension.configureDokka(project: Project, moduleName: String, javaVersion: String) {
    this.moduleName = moduleName

    dokkaSourceSets.configureEach {
        includes.from("README.md")
        jdkVersion = javaVersion.toInt()

        sourceLink {
            localDirectory = project.rootDir
            remoteUrl("https://github.com/shaksternano/gif.kt/tree/main")
            remoteLineSuffix = "#L"
        }

        externalDocumentationLinks.register("kotlinx-coroutines") {
            url("https://kotlinlang.org/api/kotlinx.coroutines")
        }

        externalDocumentationLinks.register("kotlinx-io") {
            url("https://kotlinlang.org/api/kotlinx-io")
        }
    }
}
