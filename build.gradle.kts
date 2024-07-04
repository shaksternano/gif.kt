plugins {
    kotlin("jvm") version "2.0.0"
}

group = "io.github.shaksternano.gifcodec"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-io-core:0.4.0")

    testImplementation(kotlin("test"))
}

tasks {
    test {
        useJUnitPlatform()
    }
}
