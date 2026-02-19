package io.github.fenrur.vaadin.codegen

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * KSP processor provider for Vaadin DSL code generation.
 *
 * This is the entry point for the KSP processor. It creates instances of
 * [VaadinDslProcessor] with the appropriate configuration.
 */
class VaadinDslProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val mode = environment.options["vaadindsl.mode"]?.uppercase()?.let {
            try {
                ContainerMode.valueOf(it)
            } catch (e: IllegalArgumentException) {
                environment.logger.warn("Invalid mode '$it', using QUARKUS")
                ContainerMode.QUARKUS
            }
        } ?: ContainerMode.QUARKUS

        environment.logger.info("Vaadin DSL Codegen: Using $mode mode")

        return VaadinDslProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            mode = mode
        )
    }
}
