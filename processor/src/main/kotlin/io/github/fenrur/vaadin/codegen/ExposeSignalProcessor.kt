package io.github.fenrur.vaadin.codegen

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate

/**
 * KSP processor that generates extension functions for properties annotated with @ExposeSignal.
 *
 * For each property of type `BindableMutableSignal<T>` or `BindableSignal<T>` annotated with `@ExposeSignal`,
 * generates an extension function that binds a signal to the property:
 * - `BindableMutableSignal<T>` → `MutableSignal<T>` parameter
 * - `BindableSignal<T>` → `Signal<T>` parameter
 */
class ExposeSignalProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    companion object {
        private const val EXPOSE_SIGNAL_ANNOTATION = "io.github.fenrur.vaadin.codegen.ExposeSignal"
        private const val BINDABLE_MUTABLE_SIGNAL_QUALIFIED = "io.github.fenrur.signal.BindableMutableSignal"
        private const val BINDABLE_SIGNAL_QUALIFIED = "io.github.fenrur.signal.BindableSignal"
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(EXPOSE_SIGNAL_ANNOTATION)

        // Filter out invalid symbols for deferred processing
        val deferred = symbols.filter { !it.validate() }.toList()

        // Group properties by their containing class
        val propertiesByClass = symbols
            .filter { it is KSPropertyDeclaration && it.validate() }
            .map { it as KSPropertyDeclaration }
            .filter { property ->
                validateProperty(property)
            }
            .groupBy { it.parentDeclaration as? KSClassDeclaration }
            .filterKeys { it != null }
            .mapKeys { it.key!! }

        // Generate extensions for each class
        propertiesByClass.forEach { (classDecl, properties) ->
            generateExtensions(classDecl, properties)
        }

        return deferred
    }

    /**
     * Validates that the property meets all requirements:
     * - Must be public (not internal, private or protected)
     * - Must be of type BindableMutableSignal<T> or BindableSignal<T>
     *
     * @return true if valid, false if invalid (and logs error)
     */
    private fun validateProperty(property: KSPropertyDeclaration): Boolean {
        val propertyName = property.simpleName.asString()
        val containingClass = property.parentDeclaration?.simpleName?.asString() ?: "<unknown>"

        // Check visibility - only public is allowed
        val modifiers = property.modifiers
        val isPrivate = Modifier.PRIVATE in modifiers
        val isProtected = Modifier.PROTECTED in modifiers
        val isInternal = Modifier.INTERNAL in modifiers

        if (isPrivate) {
            logger.error(
                "@ExposeSignal property '$propertyName' in class '$containingClass' must be public, but is private. " +
                "Extension functions cannot access private members.",
                property
            )
            return false
        }

        if (isProtected) {
            logger.error(
                "@ExposeSignal property '$propertyName' in class '$containingClass' must be public, but is protected. " +
                "Extension functions cannot access protected members.",
                property
            )
            return false
        }

        if (isInternal) {
            logger.error(
                "@ExposeSignal property '$propertyName' in class '$containingClass' must be public, but is internal. " +
                "Extension functions cannot access internal members from generated code.",
                property
            )
            return false
        }

        // Check type - must be BindableMutableSignal<T> or BindableSignal<T>
        val propertyType = property.type.resolve()
        val typeQualifiedName = propertyType.declaration.qualifiedName?.asString()

        if (typeQualifiedName != BINDABLE_MUTABLE_SIGNAL_QUALIFIED && typeQualifiedName != BINDABLE_SIGNAL_QUALIFIED) {
            logger.error(
                "@ExposeSignal property '$propertyName' in class '$containingClass' must be of type BindableMutableSignal<T> or BindableSignal<T>, " +
                "but is '$typeQualifiedName'.",
                property
            )
            return false
        }

        // Check that we have exactly one type argument
        if (propertyType.arguments.isEmpty()) {
            logger.error(
                "@ExposeSignal property '$propertyName' in class '$containingClass' must have a type parameter, " +
                "e.g., BindableMutableSignal<String> or BindableSignal<String>.",
                property
            )
            return false
        }

        return true
    }

    private fun generateExtensions(
        classDecl: KSClassDeclaration,
        properties: List<KSPropertyDeclaration>
    ) {
        val className = classDecl.simpleName.asString()
        val packageName = classDecl.packageName.asString()
        val extensionsFileName = "${className}SignalExtensions"

        logger.info("Generating signal extensions for: $packageName.$className (${properties.size} properties)")

        // Collect all imports needed
        val imports = mutableSetOf<String>()

        // Add signal type imports based on property types
        properties.forEach { property ->
            val propertyType = property.type.resolve()
            val typeQualifiedName = propertyType.declaration.qualifiedName?.asString()

            if (typeQualifiedName == BINDABLE_MUTABLE_SIGNAL_QUALIFIED) {
                imports.add("io.github.fenrur.signal.MutableSignal")
            } else if (typeQualifiedName == BINDABLE_SIGNAL_QUALIFIED) {
                imports.add("io.github.fenrur.signal.Signal")
            }

            // Add imports for type arguments
            val typeArg = propertyType.arguments.firstOrNull()?.type?.resolve()
            if (typeArg != null) {
                addTypeImports(typeArg, imports)
            }
        }

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(
                aggregating = true,
                sources = (properties.mapNotNull { it.containingFile } + listOfNotNull(classDecl.containingFile)).toTypedArray()
            ),
            packageName = packageName,
            fileName = extensionsFileName
        )

        file.bufferedWriter().use { writer ->
            writer.appendLine("package $packageName")
            writer.appendLine()

            // Write imports
            imports.sorted().forEach { import ->
                writer.appendLine("import $import")
            }
            writer.appendLine()

            // Generate extension function for each property
            properties.forEach { property ->
                writeExtensionFunction(writer, className, property)
                writer.appendLine()
            }
        }

        logger.info("Generated: $packageName.$extensionsFileName")
    }

    private fun writeExtensionFunction(
        writer: java.io.BufferedWriter,
        className: String,
        property: KSPropertyDeclaration
    ) {
        val propertyName = property.simpleName.asString()
        val propertyType = property.type.resolve()
        val typeQualifiedName = propertyType.declaration.qualifiedName?.asString()
        val typeArg = propertyType.arguments.firstOrNull()?.type?.resolve()
        val typeArgName = typeArg?.toSimpleTypeName() ?: "Any"

        val signalType = if (typeQualifiedName == BINDABLE_MUTABLE_SIGNAL_QUALIFIED) {
            "MutableSignal"
        } else {
            "Signal"
        }

        writer.appendLine("/**")
        writer.appendLine(" * Binds the given [signal] to the [$propertyName] property.")
        writer.appendLine(" *")
        writer.appendLine(" * @param signal the signal to bind")
        writer.appendLine(" */")
        writer.appendLine("fun $className.$propertyName(signal: $signalType<$typeArgName>) {")
        writer.appendLine("    this.$propertyName.bindTo(signal)")
        writer.appendLine("}")
    }

    private fun addTypeImports(type: KSType, imports: MutableSet<String>) {
        val qualifiedName = type.declaration.qualifiedName?.asString()
        if (qualifiedName != null && !isBuiltinType(qualifiedName)) {
            imports.add(qualifiedName)
        }

        // Recursively add imports for type arguments
        type.arguments.forEach { arg ->
            arg.type?.resolve()?.let { addTypeImports(it, imports) }
        }
    }

    private fun KSType.toSimpleTypeName(): String {
        val baseName = declaration.simpleName.asString()
        val args = arguments.mapNotNull { arg ->
            arg.type?.resolve()?.toSimpleTypeName()
        }
        val nullable = if (isMarkedNullable) "?" else ""

        return if (args.isEmpty()) {
            "$baseName$nullable"
        } else {
            "$baseName<${args.joinToString(", ")}>$nullable"
        }
    }

    private fun isBuiltinType(qualifiedName: String): Boolean {
        return qualifiedName.startsWith("kotlin.") ||
                qualifiedName.startsWith("java.lang.")
    }
}
