package io.github.fenrur.vaadin.codegen

/**
 * Marks a constructor parameter as a DI-injected dependency.
 *
 * When used in a class annotated with [GenDsl], this annotation indicates that
 * the parameter should be injected by the DI container (Quarkus Arc or Spring)
 * rather than being a DSL function parameter.
 *
 * By default, all constructor parameters are treated as DSL parameters.
 * Only parameters explicitly annotated with `@GenDslInject` are treated as
 * injected dependencies.
 *
 * When at least one parameter is annotated with `@GenDslInject`, a factory class
 * is generated to manage the dependency injection. When no parameters are annotated,
 * the component is instantiated directly without a factory.
 *
 * Example:
 * ```kotlin
 * @GenDsl
 * class CustomButton(
 *     @GenDslInject private val logger: Logger,  // Injected by DI container
 *     text: String,                              // DSL parameter (default)
 *     enabled: Boolean = true                    // DSL parameter with default value
 * ) : Button(text)
 * ```
 *
 * @see GenDsl
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class GenDslInject
