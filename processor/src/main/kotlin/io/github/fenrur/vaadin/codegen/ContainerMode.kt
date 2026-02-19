package io.github.fenrur.vaadin.codegen

/**
 * The dependency injection container mode.
 */
enum class ContainerMode {
    /**
     * Quarkus mode using Arc container.
     * Generated code uses: Arc.container().instance(FactoryClass::class.java).get()
     */
    QUARKUS,

    /**
     * Spring mode using VaadinDslApplicationContextHolder.
     * Generated code uses: VaadinDslApplicationContextHolder.getBean(FactoryClass::class.java)
     */
    SPRING
}
