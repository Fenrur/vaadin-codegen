plugins {
    kotlin("jvm") version "2.1.20" apply false
    id("org.jetbrains.dokka") version "2.1.0" apply false
    id("com.vanniktech.maven.publish") version "0.34.0" apply false
}

allprojects {
    group = "io.github.fenrur.vaadin-codegen"
    version = System.getenv("VERSION") ?: "2.0.0"

    repositories {
        mavenCentral()
    }
}
