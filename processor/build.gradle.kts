plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation(project(":library"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.20-1.0.32")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.github.fenrur.vaadin-codegen", "processor", version.toString())

    pom {
        name.set("Vaadin DSL Codegen Processor")
        description.set("KSP processor for generating Vaadin DSL factory classes")
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
