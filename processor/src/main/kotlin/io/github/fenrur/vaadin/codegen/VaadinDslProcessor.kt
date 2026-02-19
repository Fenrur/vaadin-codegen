package io.github.fenrur.vaadin.codegen

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.validate
import java.io.File

/**
 * KSP processor that generates factory classes and DSL extension functions
 * for classes annotated with @GenDsl.
 */
class VaadinDslProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val mode: ContainerMode
) : SymbolProcessor {

    companion object {
        private const val GEN_DSL_ANNOTATION = "io.github.fenrur.vaadin.codegen.GenDsl"
        private const val GEN_DSL_INJECT_ANNOTATION = "io.github.fenrur.vaadin.codegen.GenDslInject"

        private val VAADIN_COMPONENT_PACKAGES = setOf(
            "com.vaadin.flow.component",
            "com.vaadin.flow.component.button",
            "com.vaadin.flow.component.textfield",
            "com.vaadin.flow.component.html",
            "com.vaadin.flow.component.orderedlayout",
            "com.vaadin.flow.component.formlayout",
            "com.vaadin.flow.component.grid",
            "com.vaadin.flow.component.dialog",
            "com.vaadin.flow.component.notification",
            "com.vaadin.flow.component.tabs",
            "com.vaadin.flow.component.accordion",
            "com.vaadin.flow.component.details",
            "com.vaadin.flow.component.upload",
            "com.vaadin.flow.component.combobox",
            "com.vaadin.flow.component.select",
            "com.vaadin.flow.component.checkbox",
            "com.vaadin.flow.component.radiobutton",
            "com.vaadin.flow.component.listbox",
            "com.vaadin.flow.component.datepicker",
            "com.vaadin.flow.component.timepicker",
            "com.vaadin.flow.component.datetimepicker",
            "com.vaadin.flow.component.splitlayout",
            "com.vaadin.flow.component.applayout",
            "com.vaadin.flow.component.avatar",
            "com.vaadin.flow.component.contextmenu",
            "com.vaadin.flow.component.menubar",
            "com.vaadin.flow.component.messages",
            "com.vaadin.flow.component.progressbar",
            "com.vaadin.flow.component.richtexteditor",
            "com.vaadin.flow.component.treegrid",
            "com.vaadin.flow.component.virtuallist",
            // Additional Vaadin component packages
            "com.vaadin.flow.component.icon",
            "com.vaadin.flow.component.confirmdialog",
            "com.vaadin.flow.component.login",
            "com.vaadin.flow.component.sidenav",
            "com.vaadin.flow.component.map",
            "com.vaadin.flow.component.charts",
            "com.vaadin.flow.component.board",
            "com.vaadin.flow.component.cookieconsent",
            "com.vaadin.flow.component.crud",
            "com.vaadin.flow.component.gridpro",
            "com.vaadin.flow.component.spreadsheet",
            "com.vaadin.flow.component.customfield",
            "com.vaadin.flow.component.inputfield",
            "com.vaadin.flow.component.shared"
        )
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(GEN_DSL_ANNOTATION)

        // Filter out invalid symbols for deferred processing
        val deferred = symbols.filter { !it.validate() }.toList()

        // Process valid class declarations
        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { symbol ->
                val classDecl = symbol as KSClassDeclaration
                processClass(classDecl)
            }

        return deferred
    }

    private fun processClass(classDecl: KSClassDeclaration) {
        val className = classDecl.simpleName.asString()
        val packageName = classDecl.packageName.asString()
        val factoryClassName = "${className}Factory"

        logger.info("Processing @GenDsl class: $packageName.$className")

        val constructor = classDecl.primaryConstructor
        if (constructor == null) {
            logger.error("Class $className must have a primary constructor", classDecl)
            return
        }

        // Read source file to extract default values
        val sourceDefaults = extractDefaultValuesFromSource(classDecl)

        // Analyze constructor parameters
        val params = constructor.parameters.map { param ->
            val paramName = param.name?.asString() ?: "unknown"
            ConstructorParamInfo(
                name = paramName,
                type = param.type.resolve(),
                typeName = param.type.resolve().toTypeName(),
                isInjected = param.hasAnnotation(GEN_DSL_INJECT_ANNOTATION),
                hasDefault = param.hasDefault,
                defaultValue = if (param.hasDefault) sourceDefaults[paramName] else null
            )
        }

        // Check if class extends Vaadin Component
        val extendsComponent = isVaadinComponent(classDecl)

        logger.info("  - Extends Component: $extendsComponent")
        logger.info("  - Constructor params: ${params.size}")
        logger.info("  - Injected params: ${params.count { it.isInjected }}")

        // Generate code
        generateFactory(
            classDecl = classDecl,
            packageName = packageName,
            className = className,
            factoryClassName = factoryClassName,
            params = params,
            extendsComponent = extendsComponent
        )
    }

    private fun generateFactory(
        classDecl: KSClassDeclaration,
        packageName: String,
        className: String,
        factoryClassName: String,
        params: List<ConstructorParamInfo>,
        extendsComponent: Boolean
    ) {
        val injectedParams = params.filter { it.isInjected }
        val dslParams = params.filter { !it.isInjected }
        val dslFunctionName = className.replaceFirstChar { it.lowercase() }

        if (injectedParams.isEmpty()) {
            // No injected params: generate DSL-only file (no factory class)
            generateDslOnly(classDecl, packageName, className, dslFunctionName, dslParams, params, extendsComponent)
        } else {
            // Has injected params: generate factory class + DSL function
            generateFactoryWithDsl(classDecl, packageName, className, factoryClassName, dslFunctionName, injectedParams, dslParams, params, extendsComponent)
        }
    }

    private fun generateDslOnly(
        classDecl: KSClassDeclaration,
        packageName: String,
        className: String,
        dslFunctionName: String,
        dslParams: List<ConstructorParamInfo>,
        allParams: List<ConstructorParamInfo>,
        extendsComponent: Boolean
    ) {
        val fileName = "${className}Dsl"

        val imports = mutableSetOf<String>()
        imports.add("io.github.fenrur.vaadin.codegen.VaadinDsl")

        if (extendsComponent) {
            imports.add("com.vaadin.flow.component.HasComponents")
        }

        // Add imports for DSL parameter types
        dslParams.forEach { param ->
            val qualifiedName = param.type.declaration.qualifiedName?.asString()
            if (qualifiedName != null && !isBuiltinType(qualifiedName)) {
                imports.add(qualifiedName)
            }
        }

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true, sources = arrayOf(classDecl.containingFile!!)),
            packageName = packageName,
            fileName = fileName
        )

        file.bufferedWriter().use { writer ->
            writer.appendLine("package $packageName")
            writer.appendLine()

            imports.sorted().forEach { import ->
                writer.appendLine("import $import")
            }
            writer.appendLine()

            if (extendsComponent) {
                writeDirectDslFunction(writer, className, dslFunctionName, dslParams)
            }
        }

        logger.info("Generated: $packageName.$fileName")
    }

    private fun generateFactoryWithDsl(
        classDecl: KSClassDeclaration,
        packageName: String,
        className: String,
        factoryClassName: String,
        dslFunctionName: String,
        injectedParams: List<ConstructorParamInfo>,
        dslParams: List<ConstructorParamInfo>,
        allParams: List<ConstructorParamInfo>,
        extendsComponent: Boolean
    ) {
        val imports = mutableSetOf<String>()
        imports.add("io.github.fenrur.vaadin.codegen.VaadinDsl")

        when (mode) {
            ContainerMode.QUARKUS -> {
                imports.add("io.quarkus.arc.Arc")
                imports.add("io.quarkus.arc.Unremovable")
                imports.add("jakarta.enterprise.context.ApplicationScoped")
            }
            ContainerMode.SPRING -> {
                imports.add("io.github.fenrur.vaadin.codegen.VaadinDslApplicationContextHolder")
                imports.add("org.springframework.stereotype.Component")
            }
        }

        if (extendsComponent) {
            imports.add("com.vaadin.flow.component.HasComponents")
        }

        // Add imports for parameter types
        allParams.forEach { param ->
            val qualifiedName = param.type.declaration.qualifiedName?.asString()
            if (qualifiedName != null && !isBuiltinType(qualifiedName)) {
                imports.add(qualifiedName)
            }
        }

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true, sources = arrayOf(classDecl.containingFile!!)),
            packageName = packageName,
            fileName = factoryClassName
        )

        file.bufferedWriter().use { writer ->
            writer.appendLine("package $packageName")
            writer.appendLine()

            imports.sorted().forEach { import ->
                writer.appendLine("import $import")
            }
            writer.appendLine()

            writeFactoryClass(writer, className, factoryClassName, injectedParams, dslParams, allParams)

            if (extendsComponent) {
                writer.appendLine()
                writeDslFunction(writer, className, factoryClassName, dslFunctionName, dslParams)
            }
        }

        logger.info("Generated: $packageName.$factoryClassName")
    }

    private fun writeFactoryClass(
        writer: java.io.BufferedWriter,
        className: String,
        factoryClassName: String,
        injectedParams: List<ConstructorParamInfo>,
        dslParams: List<ConstructorParamInfo>,
        allParams: List<ConstructorParamInfo>
    ) {
        // Annotations
        when (mode) {
            ContainerMode.QUARKUS -> {
                writer.appendLine("@ApplicationScoped")
                writer.appendLine("@Unremovable")
            }
            ContainerMode.SPRING -> {
                writer.appendLine("@Component")
            }
        }

        // Class declaration
        if (injectedParams.isEmpty()) {
            writer.appendLine("class $factoryClassName {")
        } else {
            writer.appendLine("class $factoryClassName(")
            injectedParams.forEachIndexed { index, param ->
                val comma = if (index < injectedParams.size - 1) "," else ""
                writer.appendLine("    private val ${param.name}: ${param.simpleTypeName}$comma")
            }
            writer.appendLine(") {")
        }

        writer.appendLine()

        // create() method - now with default values
        if (dslParams.isEmpty()) {
            writer.appendLine("    fun create(): $className {")
        } else {
            writer.appendLine("    fun create(")
            dslParams.forEachIndexed { index, param ->
                val comma = if (index < dslParams.size - 1) "," else ""
                writer.appendLine("        ${param.paramDeclaration}$comma")
            }
            writer.appendLine("    ): $className {")
        }

        // Constructor call
        val allParamNames = allParams.joinToString(", ") { it.name }
        writer.appendLine("        return $className($allParamNames)")
        writer.appendLine("    }")

        writer.appendLine("}")
    }

    private fun writeDirectDslFunction(
        writer: java.io.BufferedWriter,
        className: String,
        dslFunctionName: String,
        dslParams: List<ConstructorParamInfo>
    ) {
        writer.appendLine("@VaadinDsl")

        if (dslParams.isEmpty()) {
            writer.appendLine("fun HasComponents.$dslFunctionName(")
            writer.appendLine("    block: $className.() -> Unit = {}")
            writer.appendLine("): $className {")
        } else {
            writer.appendLine("fun HasComponents.$dslFunctionName(")
            dslParams.forEachIndexed { index, param ->
                writer.appendLine("    ${param.paramDeclaration},")
            }
            writer.appendLine("    block: $className.() -> Unit = {}")
            writer.appendLine("): $className {")
        }

        // Direct instantiation
        val dslParamNames = dslParams.joinToString(", ") { it.name }
        if (dslParamNames.isEmpty()) {
            writer.appendLine("    val component = $className()")
        } else {
            writer.appendLine("    val component = $className($dslParamNames)")
        }

        writer.appendLine("    add(component)")
        writer.appendLine("    component.block()")
        writer.appendLine("    return component")
        writer.appendLine("}")
    }

    private fun writeDslFunction(
        writer: java.io.BufferedWriter,
        className: String,
        factoryClassName: String,
        dslFunctionName: String,
        dslParams: List<ConstructorParamInfo>
    ) {
        writer.appendLine("@VaadinDsl")

        if (dslParams.isEmpty()) {
            writer.appendLine("fun HasComponents.$dslFunctionName(")
            writer.appendLine("    block: $className.() -> Unit = {}")
            writer.appendLine("): $className {")
        } else {
            writer.appendLine("fun HasComponents.$dslFunctionName(")
            dslParams.forEachIndexed { index, param ->
                writer.appendLine("    ${param.paramDeclaration},")
            }
            writer.appendLine("    block: $className.() -> Unit = {}")
            writer.appendLine("): $className {")
        }

        // Get factory from container
        when (mode) {
            ContainerMode.QUARKUS -> {
                writer.appendLine("    val factory = Arc.container().instance($factoryClassName::class.java).get()")
            }
            ContainerMode.SPRING -> {
                writer.appendLine("    val factory = VaadinDslApplicationContextHolder.getBean($factoryClassName::class.java)")
            }
        }

        // Create component
        val dslParamNames = dslParams.joinToString(", ") { it.name }
        if (dslParamNames.isEmpty()) {
            writer.appendLine("    val component = factory.create()")
        } else {
            writer.appendLine("    val component = factory.create($dslParamNames)")
        }

        writer.appendLine("    add(component)")
        writer.appendLine("    component.block()")
        writer.appendLine("    return component")
        writer.appendLine("}")
    }

    private fun isVaadinComponent(classDecl: KSClassDeclaration): Boolean {
        // Check supertype hierarchy
        return classDecl.superTypes.any { superType ->
            val resolved = superType.resolve()
            val qualifiedName = resolved.declaration.qualifiedName?.asString() ?: ""

            // Direct check for Component
            if (qualifiedName == "com.vaadin.flow.component.Component" ||
                qualifiedName == "com.vaadin.flow.component.Composite") {
                return@any true
            }

            // Check if the supertype's package is a Vaadin component package
            val packageName = resolved.declaration.packageName.asString()
            if (VAADIN_COMPONENT_PACKAGES.contains(packageName)) {
                return@any true
            }

            // Recursively check if supertype extends Component
            val superDecl = resolved.declaration as? KSClassDeclaration
            if (superDecl != null) {
                return@any isVaadinComponent(superDecl)
            }

            false
        }
    }

    private fun KSValueParameter.hasAnnotation(annotationName: String): Boolean {
        return annotations.any { annotation ->
            annotation.annotationType.resolve().declaration.qualifiedName?.asString() == annotationName
        }
    }

    /**
     * Extracts default values from source file by parsing the constructor parameters.
     * Returns a map of parameter name to default value expression.
     */
    private fun extractDefaultValuesFromSource(classDecl: KSClassDeclaration): Map<String, String> {
        val filePath = classDecl.containingFile?.filePath ?: return emptyMap()
        val sourceFile = File(filePath)
        if (!sourceFile.exists()) return emptyMap()

        val sourceText = try {
            sourceFile.readText()
        } catch (e: Exception) {
            logger.warn("Could not read source file: $filePath")
            return emptyMap()
        }

        val className = classDecl.simpleName.asString()
        val defaults = mutableMapOf<String, String>()

        // Find the class declaration and its constructor parameters
        // Pattern: class ClassName( or class ClassName constructor(
        val classPattern = Regex("""class\s+$className\s*(?:constructor)?\s*\(([^)]*(?:\([^)]*\)[^)]*)*)\)""", RegexOption.DOT_MATCHES_ALL)
        val classMatch = classPattern.find(sourceText) ?: return emptyMap()

        val constructorParams = classMatch.groupValues[1]

        // Parse each parameter looking for default values
        // Handle nested parentheses, generics, and string literals
        var depth = 0
        var genericDepth = 0
        var inString = false
        var stringChar = ' '
        var currentParam = StringBuilder()
        val paramStrings = mutableListOf<String>()

        for (char in constructorParams) {
            when {
                !inString && (char == '"' || char == '\'') -> {
                    inString = true
                    stringChar = char
                    currentParam.append(char)
                }
                inString && char == stringChar -> {
                    inString = false
                    currentParam.append(char)
                }
                inString -> currentParam.append(char)
                char == '(' -> {
                    depth++
                    currentParam.append(char)
                }
                char == ')' -> {
                    depth--
                    currentParam.append(char)
                }
                char == '<' -> {
                    genericDepth++
                    currentParam.append(char)
                }
                char == '>' -> {
                    genericDepth--
                    currentParam.append(char)
                }
                char == ',' && depth == 0 && genericDepth == 0 -> {
                    paramStrings.add(currentParam.toString().trim())
                    currentParam = StringBuilder()
                }
                else -> currentParam.append(char)
            }
        }
        if (currentParam.isNotBlank()) {
            paramStrings.add(currentParam.toString().trim())
        }

        // Extract default values from each parameter string
        for (paramStr in paramStrings) {
            // Pattern: [annotations] [modifiers] name: Type = defaultValue
            val defaultPattern = Regex("""(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:private|protected|public|internal|val|var|override)\s+)*(\w+)\s*:\s*[^=]+=\s*(.+)$""")
            val match = defaultPattern.find(paramStr.trim())
            if (match != null) {
                val paramName = match.groupValues[1]
                var defaultValue = match.groupValues[2].trim()

                // Remove trailing line comments (// ...)
                val commentIndex = defaultValue.indexOf("//")
                if (commentIndex >= 0) {
                    defaultValue = defaultValue.substring(0, commentIndex).trim()
                }

                // Remove trailing block comments (/* ... */)
                val blockCommentPattern = Regex("""/\*.*?\*/""")
                defaultValue = defaultValue.replace(blockCommentPattern, "").trim()

                // Remove trailing commas that might have been left
                defaultValue = defaultValue.trimEnd(',').trim()

                if (defaultValue.isNotEmpty()) {
                    defaults[paramName] = defaultValue
                }
            }
        }

        return defaults
    }

    private fun KSType.toTypeName(): String {
        val baseName = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
        val args = arguments.mapNotNull { arg ->
            arg.type?.resolve()?.toTypeName()
        }
        return if (args.isEmpty()) {
            if (isMarkedNullable) "$baseName?" else baseName
        } else {
            val argsStr = args.joinToString(", ")
            if (isMarkedNullable) "$baseName<$argsStr>?" else "$baseName<$argsStr>"
        }
    }

    private fun isBuiltinType(qualifiedName: String): Boolean {
        return qualifiedName.startsWith("kotlin.") ||
                qualifiedName.startsWith("java.lang.")
    }

    private data class ConstructorParamInfo(
        val name: String,
        val type: KSType,
        val typeName: String,
        val isInjected: Boolean,
        val hasDefault: Boolean,
        val defaultValue: String? = null
    ) {
        val simpleTypeName: String
            get() {
                val baseName = type.declaration.simpleName.asString()
                val args = type.arguments.mapNotNull { arg ->
                    arg.type?.resolve()?.declaration?.simpleName?.asString()
                }
                val nullable = if (type.isMarkedNullable) "?" else ""
                return if (args.isEmpty()) {
                    "$baseName$nullable"
                } else {
                    "$baseName<${args.joinToString(", ")}>$nullable"
                }
            }

        val paramDeclaration: String
            get() = if (hasDefault && defaultValue != null) {
                "$name: $simpleTypeName = $defaultValue"
            } else {
                "$name: $simpleTypeName"
            }
    }
}
