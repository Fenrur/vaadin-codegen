package io.github.fenrur.vaadin.codegen

/**
 * Marks a class, type, or function as part of the Vaadin DSL.
 *
 * This annotation is used as a [DslMarker] to prevent implicit receivers
 * from outer scopes being used in nested DSL blocks.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
@DslMarker
annotation class VaadinDsl
