package io.github.fenrur.vaadin.codegen

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * KSP processor provider for @ExposeSignal code generation.
 *
 * This provider creates instances of [ExposeSignalProcessor] which generates
 * extension functions for signal binding.
 */
class ExposeSignalProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        environment.logger.info("ExposeSignal Codegen: Initializing processor")

        return ExposeSignalProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}
