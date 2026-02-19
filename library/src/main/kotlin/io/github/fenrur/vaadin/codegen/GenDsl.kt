package io.github.fenrur.vaadin.codegen

/**
 * Marks a class for DSL code generation.
 *
 * When applied to a class that extends a Vaadin Component, the code generator will:
 * 1. Generate a DSL extension function for [com.vaadin.flow.component.HasComponents]
 * 2. If any constructor parameter is annotated with [GenDslInject], also generate a
 *    factory class with dependency injection annotations (Quarkus Arc or Spring)
 *
 * Constructor parameters:
 * - By default, all parameters are treated as DSL function parameters
 * - Parameters annotated with [GenDslInject] are treated as DI-injected dependencies
 *
 * When no [GenDslInject] parameters exist, the component is instantiated directly
 * without a factory class.
 *
 * Example:
 * ```kotlin
 * @GenDsl
 * class CustomButton(
 *     @GenDslInject private val logger: Logger,  // Injected by DI container
 *     text: String,                              // DSL parameter (default)
 *     enabled: Boolean = true                    // DSL parameter with default
 * ) : Button(text)
 * ```
 *
 * Generated DSL usage:
 * ```kotlin
 * verticalLayout {
 *     customButton("Click me") {
 *         addClickListener { ... }
 *     }
 * }
 * ```
 *
 * @see GenDslInject
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenDsl
