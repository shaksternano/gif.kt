plugins {
    kotlin("jvm") version "2.0.0"
}

group = "io.github.shaksternano.gifcodec"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks {
    test {
        useJUnitPlatform()
    }
}
