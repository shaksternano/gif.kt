plugins {
    kotlin("jvm") version "2.1.21"
    `maven-publish`
}

group = "io.github.shaksternano"
base.archivesName.set("gifcodec")
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    val kotlinxIoVersion = "0.7.0"
    api("org.jetbrains.kotlinx:kotlinx-io-core:$kotlinxIoVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-io-okio:$kotlinxIoVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.squareup.okio:okio:3.10.2")

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
