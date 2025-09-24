package com.shakster.gifkt.gradle

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign

fun MavenPublishBaseExtension.configurePublishing(
    project: Project,
    artifactId: String,
    pomName: String,
    pomDescription: String,
) {
    publishToMavenCentral()

    val version = if (project.isRunningTask("publishToMavenLocal")) {
        "${project.version}-SNAPSHOT"
    } else {
        signAllPublications()
        project.version.toString()
    }

    coordinates(project.group.toString(), artifactId, version)

    pom {
        name = pomName
        description = pomDescription
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

private fun Project.isRunningTask(taskName: String): Boolean {
    return gradle.startParameter.taskNames.any {
        it.equals(taskName, ignoreCase = true)
    }
}
