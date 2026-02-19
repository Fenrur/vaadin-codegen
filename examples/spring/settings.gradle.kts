pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.vaadin.com/vaadin-prereleases") }
        maven { url = uri("https://jitpack.io") }
    }

    plugins {
        kotlin("jvm") version "2.1.0"
        kotlin("plugin.spring") version "2.1.0"
        id("com.google.devtools.ksp") version "2.1.0-1.0.29"
        id("org.springframework.boot") version "3.4.1"
        id("io.spring.dependency-management") version "1.1.7"
        id("com.vaadin") version "24.6.3"
        id("io.github.fenrur.vaadin-codegen") version "1.0.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://maven.vaadin.com/vaadin-prereleases") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "spring-example"
