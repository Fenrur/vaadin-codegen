plugins {
    kotlin("jvm") version "2.1.20"
    `java-gradle-plugin`
    id("org.jetbrains.dokka") version "2.1.0"
    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "io.github.fenrur.vaadin-codegen"
version = System.getenv("VERSION") ?: "2.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation(gradleApi())
    compileOnly("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.1.20-1.0.32")
}

gradlePlugin {
    plugins {
        create("vaadinDslCodegen") {
            id = "io.github.fenrur.vaadin-codegen"
            implementationClass = "io.github.fenrur.vaadin.codegen.VaadinDslCodegenPlugin"
            displayName = "Vaadin DSL Codegen"
            description = "Gradle plugin for configuring Vaadin DSL code generation"
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("Vaadin DSL Codegen Gradle Plugin")
        description.set("Gradle plugin for configuring Vaadin DSL code generation")
        url.set("https://github.com/fenrur/vaadin-codegen")
        inceptionYear.set("2025")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("fenrur")
                name.set("Livio TINNIRELLO")
            }
        }

        scm {
            url.set("https://github.com/fenrur/vaadin-codegen")
            connection.set("scm:git:git://github.com/fenrur/vaadin-codegen.git")
            developerConnection.set("scm:git:ssh://github.com/fenrur/vaadin-codegen.git")
        }
    }
}
