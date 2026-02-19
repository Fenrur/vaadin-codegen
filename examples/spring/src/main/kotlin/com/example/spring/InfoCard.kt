package com.example.spring

import io.github.fenrur.vaadin.codegen.GenDsl
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Paragraph

/**
 * Example card component without any injected dependencies.
 *
 * The processor will generate:
 * - InfoCardDsl with direct instantiation (no factory, no DI)
 * - infoCard() DSL extension function for HasComponents
 */
@GenDsl
class InfoCard(
    val title: String,
    val description: String
) : Div() {

    init {
        addClassName("info-card")
        add(H3(title))
        add(Paragraph(description))
    }
}
