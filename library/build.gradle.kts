plugins {
    kotlin("jvm")
    `java-library`
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

dependencies {
    compileOnly("org.springframework:spring-context:5.3.39")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
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
    coordinates("io.github.fenrur.vaadin-codegen", "library", version.toString())

    pom {
        name.set("Vaadin DSL Codegen Library")
        description.set("Annotations for Vaadin DSL code generation")
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
