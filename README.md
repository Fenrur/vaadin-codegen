# Vaadin Codegen

[![Maven Central](https://img.shields.io/maven-central/v/io.github.fenrur.vaadin-codegen/library)](https://central.sonatype.com/artifact/io.github.fenrur.vaadin-codegen/library)
[![Build](https://github.com/Fenrur/vaadin-codegen/actions/workflows/ci.yml/badge.svg)](https://github.com/Fenrur/vaadin-codegen/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1.20-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A KSP (Kotlin Symbol Processing) processor that generates factory classes and DSL extension functions for Vaadin components.

## Installation

### Gradle (Kotlin DSL)

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // Define KSP version here for compatibility with your Kotlin version
        id("com.google.devtools.ksp") version "2.1.20-1.0.32"
        id("io.github.fenrur.vaadin-codegen") version "2.0.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// build.gradle.kts
import io.github.fenrur.vaadin.codegen.VaadinDslCodegenExtension.Mode

plugins {
    kotlin("jvm") version "2.1.20"
    id("com.google.devtools.ksp") version "2.1.20-1.0.32" // KSP version must match your Kotlin version
    id("io.github.fenrur.vaadin-codegen") version "2.0.0"
}

dependencies {
    implementation("io.github.fenrur.vaadin-codegen:library:2.0.0")
    ksp("io.github.fenrur.vaadin-codegen:processor:2.0.0")
}

vaadinDslCodegen {
    mode = Mode.QUARKUS // or Mode.SPRING
}
```

### Gradle (Groovy DSL)

```groovy
// settings.gradle
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // Define KSP version here for compatibility with your Kotlin version
        id 'com.google.devtools.ksp' version '2.1.20-1.0.32'
        id 'io.github.fenrur.vaadin-codegen' version '2.0.0'
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// build.gradle
import io.github.fenrur.vaadin.codegen.VaadinDslCodegenExtension.Mode

plugins {
    id 'org.jetbrains.kotlin.jvm' version '2.1.20'
    id 'com.google.devtools.ksp' version '2.1.20-1.0.32' // KSP version must match your Kotlin version
    id 'io.github.fenrur.vaadin-codegen' version '2.0.0'
}

dependencies {
    implementation 'io.github.fenrur.vaadin-codegen:library:2.0.0'
    ksp 'io.github.fenrur.vaadin-codegen:processor:2.0.0'
}

vaadinDslCodegen {
    mode = Mode.QUARKUS // or Mode.SPRING
}
```

## Configuration

### Modes

| Mode | Container | Bean retrieval |
|------|-----------|----------------|
| `QUARKUS` | Arc | `Arc.container().instance(...).get()` |
| `SPRING` | Spring | `VaadinDslApplicationContextHolder.getBean(...)` |

## Spring Mode Setup

When using Spring mode, make sure `VaadinDslApplicationContextHolder` is available:

```kotlin
// This class is provided in the library module
// Just make sure Spring component scanning includes the package
@ComponentScan("io.github.fenrur.vaadin.codegen")
```

## Quarkus + KSP Circular Dependency Fix

When using KSP with Quarkus, you may encounter a circular dependency error between `kspKotlin`, `quarkusGenerateCode`, and `processResources` tasks.

See [quarkusio/quarkus#29698](https://github.com/quarkusio/quarkus/issues/29698) for more details.

### Fix for Gradle (Kotlin DSL)

Add this to your `build.gradle.kts`:

```kotlin
// Fix circular dependency between KSP and Quarkus
afterEvaluate {
    tasks.named("kspKotlin") {
        setDependsOn(dependsOn.filterNot {
            it.toString().contains("quarkusGenerateCode")
        })
    }
}
```

### Fix for Gradle (Groovy DSL)

Add this to your `build.gradle`:

```groovy
// Fix circular dependency between KSP and Quarkus (https://github.com/quarkusio/quarkus/issues/29698)
afterEvaluate {
    tasks.getByName("quarkusGenerateCode").setDependsOn(
        tasks.getByName("quarkusGenerateCode").dependsOn.findAll { dep ->
            !(dep instanceof Provider && ((Provider) dep).get().name == "processResources")
        }
    )
    tasks.getByName("quarkusGenerateCodeDev").setDependsOn(
        tasks.getByName("quarkusGenerateCodeDev").dependsOn.findAll { dep ->
            !(dep instanceof Provider && ((Provider) dep).get().name == "processResources")
        }
    )
}
```

## Quarkus Dev Mode + KSP Hot Reload

Quarkus dev mode uses its own internal compiler for hot-reload, which does **not** run KSP when source files change. This means if you modify `@GenDsl` classes, the generated DSL files won't be updated automatically.

### Solution: Run KSP in Continuous Mode

Run KSP in watch mode in one terminal, and Quarkus dev in another:

**Terminal 1 - KSP Watch:**
```bash
cd examples/quarkus
./gradlew kspKotlin --continuous
```

**Terminal 2 - Quarkus Dev:**
```bash
cd examples/quarkus
./gradlew quarkusDev
```

Now when you modify `@GenDsl` annotated classes:
1. The `--continuous` flag detects file changes and re-runs `kspKotlin`
2. KSP regenerates the DSL files in `build/generated/ksp/main/kotlin/`
3. Quarkus dev mode detects the new generated files and recompiles
4. Refresh your browser to see the changes

### Recommended build.gradle.kts Configuration

Add this to your `build.gradle.kts` for optimal KSP + Quarkus dev mode support:

```kotlin
// Register KSP generated sources
sourceSets {
    main {
        kotlin.srcDirs("build/generated/ksp/main/kotlin")
        java.srcDirs("build/generated/ksp/main/java")
    }
}

// Ensure compileKotlin always runs after kspKotlin
tasks.named("compileKotlin") {
    dependsOn("kspKotlin")
}

// Configure quarkusDev to run KSP before starting
tasks.named("quarkusDev") {
    dependsOn("kspKotlin")
}
```

## Push Configuration (WebSocket)

For reactive features and server-side push updates, configure your `AppShellConfigurator` with WebSocket transport.

### Quarkus

Create a separate class implementing `AppShellConfigurator`:

```kotlin
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.server.PWA
import com.vaadin.flow.shared.communication.PushMode
import com.vaadin.flow.shared.ui.Transport
import com.vaadin.flow.theme.Theme
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
@Push(PushMode.AUTOMATIC, transport = Transport.WEBSOCKET)
@PWA(name = "My Application", shortName = "MyApp")
class VaadinAppShellConfigurator : AppShellConfigurator
```

### Spring Boot

Add `AppShellConfigurator` to your main application class:

```kotlin
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.server.PWA
import com.vaadin.flow.shared.communication.PushMode
import com.vaadin.flow.shared.ui.Transport
import com.vaadin.flow.theme.Theme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.example.spring", "io.github.fenrur.vaadin.codegen"])
@Push(PushMode.AUTOMATIC, transport = Transport.WEBSOCKET)
@PWA(name = "My Application", shortName = "MyApp")
class Application : AppShellConfigurator

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
```

### Push Modes

| Mode | Description |
|------|-------------|
| `PushMode.AUTOMATIC` | Server pushes changes automatically when UI is modified |
| `PushMode.MANUAL` | You must call `ui.push()` manually to send updates |
| `PushMode.DISABLED` | Push is disabled |

### Transport Options

| Transport | Description |
|-----------|-------------|
| `Transport.WEBSOCKET` | Pure WebSocket connection (recommended for real-time updates) |
| `Transport.WEBSOCKET_XHR` | WebSocket with XHR fallback (default) |
| `Transport.LONG_POLLING` | HTTP long polling (for environments where WebSocket is not available) |

## Usage

### Annotations

#### @GenDsl

Marks a class for DSL code generation.

```kotlin
import io.github.fenrur.vaadin.codegen.GenDsl
import io.github.fenrur.vaadin.codegen.GenDslInject

@GenDsl
class CustomButton(
    @GenDslInject private val logger: Logger,  // Injected by DI container
    val text: String,                          // DSL parameter (default)
    val enabled: Boolean = true                // DSL parameter with default
) : Button(text) {
    init {
        isEnabled = enabled
    }
}
```

#### @GenDslInject

Marks a constructor parameter as a DI-injected dependency. By default, all constructor parameters are DSL parameters. Only parameters annotated with `@GenDslInject` are injected by the DI container.

When no `@GenDslInject` parameters exist, the component is instantiated directly without a factory class.

#### @ExposeSignal

Marks a property for signal binding extension generation. When applied to a property of type `BindableMutableSignal<T>` or `BindableSignal<T>`, the processor generates an extension function that allows binding a signal to this property.

This annotation can be used with the following optional libraries:
- [karibu-dsl](https://github.com/mvysny/karibu-dsl) — Kotlin DSL for building Vaadin UI
- [vaadin-signal](https://github.com/Fenrur/vaadin-signal) — Reactive signal bindings for Vaadin

The examples below demonstrate usage with both libraries.

**Supported types:**

| Property Type              | Generated Extension Parameter |
|----------------------------|-------------------------------|
| `BindableMutableSignal<T>` | `MutableSignal<T>`            |
| `BindableSignal<T>`        | `Signal<T>`                   |

**Visibility requirements:**
- The property **must be `public`**
- `internal`, `protected`, and `private` properties will cause a compilation error

```kotlin
import io.github.fenrur.signal.BindableMutableSignal
import io.github.fenrur.signal.BindableSignal
import io.github.fenrur.signal.bindableMutableSignalOf
import io.github.fenrur.signal.bindableSignalOf
import io.github.fenrur.vaadin.codegen.ExposeSignal
import io.github.fenrur.vaadin.codegen.GenDsl
import io.github.fenrur.vaadin.signal.textContent
import io.github.fenrur.vaadin.signal.visible
import com.github.mvysny.karibudsl.v10.h3
import com.github.mvysny.karibudsl.v10.p
import com.vaadin.flow.component.html.Div

@GenDsl
class ReactiveCard : Div() {

    @ExposeSignal
    val title: BindableMutableSignal<String> = bindableMutableSignalOf("")

    @ExposeSignal
    val description: BindableMutableSignal<String> = bindableMutableSignalOf("")

    @ExposeSignal
    val cardVisible: BindableSignal<Boolean> = bindableSignalOf(true)

    init {
        addClassName("reactive-card")
        visible(cardVisible)

        h3 {
            textContent(title)
        }
        p {
            textContent(description)
        }
    }
}
```

**Generated extensions:**
```kotlin
fun ReactiveCard.title(signal: MutableSignal<String>) {
    this.title.bindTo(signal)
}

fun ReactiveCard.description(signal: MutableSignal<String>) {
    this.description.bindTo(signal)
}

fun ReactiveCard.cardVisible(signal: Signal<Boolean>) {
    this.cardVisible.bindTo(signal)
}
```

**Usage:**
```kotlin
// Create signals for reactive state
val titleSignal = mutableSignalOf("Welcome")
val descriptionSignal = mutableSignalOf("This card updates reactively")
val isVisible = mutableSignalOf(true)

// Use the generated DSL and bind signals
reactiveCard {
    title(titleSignal)
    description(descriptionSignal)
    cardVisible(isVisible)
}

// Later, update the signals — the UI updates automatically
titleSignal.value = "Updated Title"
descriptionSignal.value = "New description content"
isVisible.value = false  // Hides the card
```

### Generated Code

**With `@GenDslInject` (factory generated) — Quarkus mode:**
```kotlin
@ApplicationScoped
@Unremovable
class CustomButtonFactory(
    private val logger: Logger
) {
    fun create(text: String, enabled: Boolean = true): CustomButton {
        return CustomButton(logger, text, enabled)
    }
}

@VaadinDsl
fun HasComponents.customButton(
    text: String,
    enabled: Boolean = true,
    block: CustomButton.() -> Unit = {}
): CustomButton {
    val factory = Arc.container().instance(CustomButtonFactory::class.java).get()
    val component = factory.create(text, enabled)
    add(component)
    component.block()
    return component
}
```

**With `@GenDslInject` (factory generated) — Spring mode:**
```kotlin
@Component
class CustomButtonFactory(
    private val logger: Logger
) {
    fun create(text: String, enabled: Boolean = true): CustomButton {
        return CustomButton(logger, text, enabled)
    }
}

@VaadinDsl
fun HasComponents.customButton(
    text: String,
    enabled: Boolean = true,
    block: CustomButton.() -> Unit = {}
): CustomButton {
    val factory = VaadinDslApplicationContextHolder.getBean(CustomButtonFactory::class.java)
    val component = factory.create(text, enabled)
    add(component)
    component.block()
    return component
}
```

**Without `@GenDslInject` (no factory, direct instantiation):**
```kotlin
// For a simple component like:
// @GenDsl class InfoCard(val title: String, val description: String) : Div()

@VaadinDsl
fun HasComponents.infoCard(
    title: String,
    description: String,
    block: InfoCard.() -> Unit = {}
): InfoCard {
    val component = InfoCard(title, description)
    add(component)
    component.block()
    return component
}
```

### DSL Usage

```kotlin
verticalLayout {
    customButton("Click me") {
        addClickListener {
            // handle click
        }
    }

    customButton("Disabled", enabled = false)
}
```

## How It Works

The processor uses KSP to:

1. Find all classes annotated with `@GenDsl`
2. Analyze the primary constructor parameters
3. Detect `@GenDslInject` annotations to separate injected dependencies from DSL params
4. Check if the class extends a Vaadin `Component` (via type resolution)
5. If `@GenDslInject` params exist: generate a factory class with DI annotations + DSL function
6. If no `@GenDslInject` params: generate only a DSL function with direct instantiation

## Generated Files Location

Generated files are placed in `build/generated/ksp/main/kotlin/`.

## Examples

The repository includes working examples for both Quarkus and Spring:

```
examples/
├── quarkus/     # Quarkus + Vaadin example
└── spring/      # Spring Boot + Vaadin example
```

### Running the Examples

Each example is a standalone Gradle project. First, publish the library to mavenLocal:
```bash
./gradlew publishToMavenLocal
```

**Quarkus:**
```bash
cd examples/quarkus
./gradlew quarkusDev
# Open http://localhost:8082
```

**Spring:**
```bash
cd examples/spring
./gradlew bootRun
# Open http://localhost:8081
```

### Example Components

Both examples include:

- **CustomButton**: A button component with DI-injected Logger and DSL parameters
- **InfoCard**: A simple card component with only DSL parameters
- **MainView**: Demonstrates using the generated DSL functions

```kotlin
// Component with DI injection (factory generated)
@GenDsl
class CustomButton(
    @GenDslInject private val logger: Logger,  // Injected by DI container
    label: String,                             // DSL parameter (default)
    val primary: Boolean = false               // DSL parameter
) : Button(label)

// Simple component (no factory, direct instantiation)
@GenDsl
class InfoCard(
    val title: String,
    val description: String
) : Div()

// Usage in a view
class MainView : VerticalLayout() {
    init {
        customButton("Click Me", primary = true) {
            addClickListener { /* ... */ }
        }
        infoCard("Title", "Description")
    }
}
```

## License

MIT License
