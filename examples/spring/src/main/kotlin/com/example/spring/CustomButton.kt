package com.example.spring

import io.github.fenrur.vaadin.codegen.GenDsl
import io.github.fenrur.vaadin.codegen.GenDslInject
import com.vaadin.flow.component.button.Button
import org.slf4j.Logger

/**
 * Example custom button component with Spring injection.
 *
 * The processor will generate:
 * - CustomButtonFactory with @Component annotation
 * - customButton() DSL extension function using VaadinDslApplicationContextHolder
 */
@GenDsl
class CustomButton(
    @GenDslInject private val logger: Logger,      // Spring injected
    label: String,                                 // DSL parameter
    val primary: Boolean = false                   // DSL parameter with default
) : Button(label) {

    init {
        if (primary) {
            addThemeName("primary")
        }
        addClickListener {
            logger.info("CustomButton clicked: $text")
        }
    }
}
