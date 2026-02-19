package io.github.fenrur.vaadin.codegen

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

/**
 * Spring ApplicationContext holder for Vaadin DSL.
 *
 * This component allows the generated DSL functions to retrieve
 * Spring beans from the application context.
 *
 * This class is only used when the plugin is configured in Spring mode.
 * Thread-safe implementation using AtomicReference.
 */
@Component
class VaadinDslApplicationContextHolder : ApplicationContextAware {

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        contextRef.set(applicationContext)
    }

    companion object {
        private val contextRef = AtomicReference<ApplicationContext>()

        @JvmStatic
        fun getContext(): ApplicationContext = contextRef.get()
            ?: throw IllegalStateException(
                "ApplicationContext not initialized. Ensure VaadinDslApplicationContextHolder " +
                "is registered as a Spring component and the application context is fully loaded."
            )

        @JvmStatic
        fun <T> getBean(beanClass: Class<T>): T = getContext().getBean(beanClass)

        @JvmStatic
        fun isInitialized(): Boolean = contextRef.get() != null
    }
}
