plugins {
    kotlin("jvm") version "2.0.20"
    id("org.jetbrains.kotlinx.atomicfu") version "0.25.0"
    `maven-publish`
}

group = "io.github.shaksternano"
base.archivesName.set("gifcodec")
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-io-core:0.5.3")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC.2")
    implementation("org.jetbrains.kotlinx:atomicfu:0.25.0")

    testImplementation(kotlin("test"))
}

val javadoc: Javadoc by tasks

val javadocJar = task<Jar>("javadocJar") {
    from(javadoc.destinationDir)
    archiveClassifier.set("javadoc")

    dependsOn(javadoc)
}

val sourcesJar = task<Jar>("sourcesJar") {
    from(sourceSets["main"].allSource)
    archiveClassifier.set("sources")
}

tasks {
    build {
        dependsOn(javadocJar)
        dependsOn(sourcesJar)
    }

    test {
        useJUnitPlatform()
    }
}

kotlin {
    jvmToolchain(8)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            from(components["java"])
            artifactId = base.archivesName.get()

            artifact(javadocJar)
            artifact(sourcesJar)
        }
    }
}
