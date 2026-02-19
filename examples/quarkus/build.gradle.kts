import io.github.fenrur.vaadin.codegen.VaadinDslCodegenExtension.Mode

plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    id("com.google.devtools.ksp")
    id("io.quarkus")
    id("com.vaadin")
    id("io.github.fenrur.vaadin-codegen")
}

val vaadinVersion = "24.6.3"
val quarkusVersion = "3.17.7"
val vaadinCodegenVersion = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // BOM
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))

    // Vaadin Quarkus
    implementation("com.vaadin:vaadin-quarkus-extension:$vaadinVersion") {
        exclude(group = "com.vaadin", module = "copilot")
    }

    // Vaadin DSL Codegen library
    implementation("io.github.fenrur.vaadin-codegen:library:$vaadinCodegenVersion")
    ksp("io.github.fenrur.vaadin-codegen:processor:$vaadinCodegenVersion")

    // Signal library for @ExposeSignal
    implementation("io.github.fenrur:signal:1.0.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")


    // Test
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

vaadinDslCodegen {
    mode = Mode.QUARKUS
}

allOpen {
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.ws.rs.Path")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

vaadin {
    pnpmEnable = true
}

// Fix circular dependency between KSP and Quarkus
afterEvaluate {
    tasks.named("kspKotlin") {
        setDependsOn(dependsOn.filterNot {
            it.toString().contains("quarkusGenerateCode")
        })
    }
}

// Register KSP generated sources for Quarkus dev mode
sourceSets {
    main {
        kotlin.srcDirs("build/generated/ksp/main/kotlin")
        java.srcDirs("build/generated/ksp/main/java")
    }
}

// Ensure compileKotlin always runs after kspKotlin
tasks.named("compileKotlin") {
    dependsOn("kspKotlin")
}

// Configure quarkusDev to run KSP before starting
tasks.named("quarkusDev") {
    dependsOn("kspKotlin")
}

// ===============================================================
// KSP Hot Reload Support for Quarkus Dev Mode
// ===============================================================
//
// Quarkus dev mode uses its own internal compiler and does NOT
// run KSP when source files change. To enable KSP hot reload:
//
// Option 1: Run kspKotlin in continuous mode in a separate terminal:
//   ./gradlew kspKotlin --continuous
//
// Option 2: Use the custom kspWatch task (runs in background):
//   ./gradlew kspWatch
//
// Then in another terminal, run quarkusDev normally:
//   ./gradlew quarkusDev
// ===============================================================

// Task to run KSP in continuous/watch mode
tasks.register("kspWatch") {
    group = "development"
    description = "Run KSP in continuous mode for hot reload (run in separate terminal)"

    doLast {
        println("""
            |
            |========================================
            | KSP Watch Mode Instructions
            |========================================
            |
            | To enable KSP hot reload with Quarkus:
            |
            | 1. In THIS terminal, run:
            |    ./gradlew kspKotlin --continuous
            |
            | 2. In ANOTHER terminal, run:
            |    ./gradlew quarkusDev
            |
            | Now when you modify @GenDsl classes,
            | KSP will regenerate the DSL files and
            | Quarkus will pick up the changes.
            |
            |========================================
        """.trimMargin())
    }
}
