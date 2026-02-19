import io.github.fenrur.vaadin.codegen.VaadinDslCodegenExtension.Mode

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("com.google.devtools.ksp")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.vaadin")
    id("io.github.fenrur.vaadin-codegen")
}

val vaadinVersion = "24.6.3"
val vaadinCodegenVersion = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencyManagement {
    imports {
        mavenBom("com.vaadin:vaadin-bom:$vaadinVersion")
    }
}

dependencies {
    // Vaadin Spring Boot
    implementation("com.vaadin:vaadin-spring-boot-starter")

    // Vaadin DSL Codegen library
    implementation("io.github.fenrur.vaadin-codegen:library:$vaadinCodegenVersion")
    ksp("io.github.fenrur.vaadin-codegen:processor:$vaadinCodegenVersion")

    // Signal library for @ExposeSignal
    implementation("io.github.fenrur:signal:1.0.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")

    // Dev tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

vaadinDslCodegen {
    mode = Mode.SPRING
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

vaadin {
    pnpmEnable = true
}
