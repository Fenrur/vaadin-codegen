package com.example.spring

import io.github.fenrur.signal.BindableMutableSignal
import io.github.fenrur.signal.BindableSignal
import io.github.fenrur.signal.bindableMutableSignalOf
import io.github.fenrur.signal.bindableSignalOf
import io.github.fenrur.vaadin.codegen.ExposeSignal
import io.github.fenrur.vaadin.codegen.GenDsl
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Paragraph

/**
 * Example component demonstrating @ExposeSignal annotation.
 *
 * The processor will generate:
 * - SignalCardDsl with direct instantiation (no factory, no DI)
 * - signalCard() DSL extension function
 * - title(signal) extension function (from @ExposeSignal on BindableMutableSignal title property)
 * - content(signal) extension function (from @ExposeSignal on BindableMutableSignal content property)
 * - visible(signal) extension function (from @ExposeSignal on BindableSignal visible property)
 */
@GenDsl
class SignalCard : Div() {

    @ExposeSignal
    val title: BindableMutableSignal<String> = bindableMutableSignalOf("")

    @ExposeSignal
    val content: BindableMutableSignal<String> = bindableMutableSignalOf("")

    @ExposeSignal
    val visible: BindableSignal<Boolean> = bindableSignalOf(true)

    private val titleElement = H3()
    private val contentElement = Paragraph()

    init {
        addClassName("signal-card")
        add(titleElement)
        add(contentElement)
    }
}
