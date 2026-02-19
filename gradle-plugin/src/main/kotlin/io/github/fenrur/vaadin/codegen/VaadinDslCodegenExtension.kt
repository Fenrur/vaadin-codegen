package io.github.fenrur.vaadin.codegen

import org.gradle.api.provider.Property

/**
 * Configuration extension for Vaadin DSL Codegen.
 *
 * Example usage in build.gradle.kts:
 * ```kotlin
 * vaadinDslCodegen {
 *     mode = Mode.QUARKUS
 * }
 * ```
 */
abstract class VaadinDslCodegenExtension {

    /**
     * The container mode for code generation.
     * Defaults to [Mode.QUARKUS].
     */
    abstract val mode: Property<Mode>

    /**
     * Supported DI container modes.
     */
    enum class Mode {
        /**
         * Generate code for Quarkus Arc container.
         * Uses @ApplicationScoped, @Unremovable annotations and Arc.container().instance(...).get()
         */
        QUARKUS,

        /**
         * Generate code for Spring container.
         * Uses @Component annotation and VaadinDslApplicationContextHolder.getBean(...)
         */
        SPRING
    }
}
