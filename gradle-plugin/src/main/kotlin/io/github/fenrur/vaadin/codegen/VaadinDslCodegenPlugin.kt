package io.github.fenrur.vaadin.codegen

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin that configures Vaadin DSL code generation.
 *
 * This plugin:
 * 1. Requires the KSP plugin to be applied by the consumer (for version compatibility)
 * 2. Creates the `vaadinDslCodegen` extension for configuration
 * 3. Configures KSP arguments based on the extension settings
 *
 * Example usage:
 * ```kotlin
 * plugins {
 *     id("com.google.devtools.ksp") version "2.2.0-2.0.2"  // Apply KSP with your Kotlin version
 *     id("io.github.fenrur.vaadin-codegen") version "1.0.0"
 * }
 *
 * vaadinDslCodegen {
 *     mode = Mode.QUARKUS
 * }
 * ```
 */
class VaadinDslCodegenPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Create the extension
        val extension = project.extensions.create(
            "vaadinDslCodegen",
            VaadinDslCodegenExtension::class.java
        )

        // Set default value using convention
        extension.mode.convention(VaadinDslCodegenExtension.Mode.QUARKUS)

        // Configure KSP arguments after project evaluation
        project.afterEvaluate {
            // Check that KSP plugin is applied
            if (!project.plugins.hasPlugin("com.google.devtools.ksp")) {
                throw GradleException(
                    "The 'com.google.devtools.ksp' plugin must be applied before 'io.github.fenrur.vaadin-codegen'. " +
                    "Please add: id(\"com.google.devtools.ksp\") version \"<version>\" to your plugins block."
                )
            }

            project.extensions.findByType(KspExtension::class.java)?.let { ksp ->
                ksp.arg("vaadindsl.mode", extension.mode.get().name)
            }
        }
    }
}
