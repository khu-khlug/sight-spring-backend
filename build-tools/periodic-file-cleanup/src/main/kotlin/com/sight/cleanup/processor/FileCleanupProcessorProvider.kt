package com.sight.cleanup.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.validate

class FileCleanupProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        FileCleanupProcessor(environment.codeGenerator, environment.logger)
}

private class FileCleanupProcessor(
    private val generator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private var generated = false
    private var invalid = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()
        val predicates = resolver.getSymbolsWithAnnotation(PREDICATE).toList()
        val releases = resolver.getSymbolsWithAnnotation(RELEASE).toList()
        val deferred = (predicates + releases).filterNot { it.validate() }
        if (deferred.isNotEmpty()) return deferred
        val targets = collect(predicates, PREDICATE, "com.sight.service.FileCleanupContext", "kotlin.Boolean")
        val hooks = collect(releases, RELEASE, "com.sight.domain.file.FileReference", "kotlin.Unit")
        hooks.forEach { (type, hook) ->
            val target = targets[type]
            val hookOwner = (hook.parentDeclaration as? KSClassDeclaration)?.qualifiedName?.asString()
            val targetOwner = (target?.parentDeclaration as? KSClassDeclaration)?.qualifiedName?.asString()
            if (target == null || hookOwner != targetOwner) {
                error("부가 행 해제 함수는 같은 서비스의 정리 판단 함수와 같은 참조 종류를 지정해야 합니다: $type", hook)
            }
        }
        if (invalid) return emptyList()

        val services = targets.values.map { it.parentDeclaration as KSClassDeclaration }.distinctBy { it.qualifiedName!!.asString() }
            .sortedBy { it.qualifiedName!!.asString() }
        val names = services.mapIndexed { index, service -> service.qualifiedName!!.asString() to "service$index" }.toMap()
        val files = resolver.getAllFiles().toList().toTypedArray()
        generator.createNewFile(Dependencies(true, *files), "com.sight.service", "GeneratedFileCleanupRegistry").bufferedWriter().use { out ->
            out.appendLine("package com.sight.service")
            out.appendLine()
            out.appendLine("@org.springframework.stereotype.Service")
            if (services.isEmpty()) {
                out.appendLine("class GeneratedFileCleanupRegistry : FileCleanupRegistry {")
                out.appendLine("    override val bindings: List<FileCleanupBinding> = emptyList()")
                out.appendLine("}")
                return@use
            }
            out.appendLine("class GeneratedFileCleanupRegistry(")
            services.forEach { service ->
                val type = service.qualifiedName!!.asString()
                out.appendLine("    ${names.getValue(type)}: $type,")
            }
            out.appendLine(") : FileCleanupRegistry {")
            out.appendLine("    override val bindings: List<FileCleanupBinding> =")
            out.appendLine("        listOf(")
            targets.toSortedMap().forEach { (type, function) ->
                val service = names.getValue((function.parentDeclaration as KSClassDeclaration).qualifiedName!!.asString())
                val predicate = "$service::`${function.simpleName.asString()}`"
                val release = hooks[type]?.let { "$service::`${it.simpleName.asString()}`" } ?: "null"
                out.appendLine("            FileCleanupBinding(")
                out.appendLine("                com.sight.domain.file.FileReferenceType.$type,")
                out.appendLine("                $predicate,")
                out.appendLine("                $release,")
                out.appendLine("            ),")
            }
            out.appendLine("        )")
            out.appendLine("}")
        }
        generated = true
        return emptyList()
    }

    private fun collect(
        symbols: List<KSAnnotated>,
        annotation: String,
        parameter: String,
        result: String,
    ): Map<String, KSFunctionDeclaration> {
        val functions = mutableMapOf<String, KSFunctionDeclaration>()
        symbols.forEach { symbol ->
            val function = symbol as? KSFunctionDeclaration
            val owner = function?.parentDeclaration as? KSClassDeclaration
            if (function == null || owner == null || owner.classKind != ClassKind.CLASS || owner.typeParameters.isNotEmpty() ||
                owner.qualifiedName == null || owner.modifiers.any { it in setOf(Modifier.PRIVATE, Modifier.PROTECTED, Modifier.ABSTRACT, Modifier.INNER) } ||
                owner.annotations.none { it.annotationType.resolve().declaration.qualifiedName?.asString() == "org.springframework.stereotype.Service" }
            ) {
                error("파일 정리 함수는 구체적인 @Service 클래스의 멤버여야 합니다.", symbol)
                return@forEach
            }
            if (function.modifiers.any { it in setOf(Modifier.PRIVATE, Modifier.PROTECTED, Modifier.ABSTRACT, Modifier.SUSPEND) } ||
                function.extensionReceiver != null || function.typeParameters.isNotEmpty() || function.parameters.size != 1 ||
                !matches(function.parameters.singleOrNull()?.type?.resolve(), parameter) || !matches(function.returnType?.resolve(), result)
            ) {
                error("파일 정리 함수 형식은 ($parameter) -> $result 이어야 합니다.", function)
                return@forEach
            }
            val value = function.annotations.first { it.annotationType.resolve().declaration.qualifiedName?.asString() == annotation }
                .arguments.first { it.name?.asString() == "referenceTypes" }.value
            val values =
                when (value) {
                    is List<*> -> value
                    is Array<*> -> value.toList()
                    null -> emptyList<Any?>()
                    else -> listOf(value)
                }
            val entries = values.mapNotNull { enumName(it) }
            if (entries.isEmpty() || entries.size != values.size) error("파일 정리 함수의 참조 종류를 해석할 수 없습니다.", function)
            entries.forEach { type ->
                if (functions.putIfAbsent(type, function) != null) error("파일 정리 함수의 참조 종류가 중복되었습니다: $type", function)
            }
        }
        return functions
    }

    private fun enumName(value: Any?): String? =
        when (value) {
            is KSClassDeclaration -> value.simpleName.asString()
            is KSType -> value.declaration.simpleName.asString()
            is KSName -> value.asString().substringAfterLast('.')
            is Enum<*> -> value.name
            else -> null
        }

    private fun matches(type: KSType?, name: String): Boolean =
        type != null && type.declaration.qualifiedName?.asString() == name && type.nullability == Nullability.NOT_NULL

    private fun error(message: String, symbol: KSAnnotated) {
        invalid = true
        logger.error(message, symbol)
    }

    companion object {
        const val PREDICATE = "com.sight.service.PeriodicFileCleanup"
        const val RELEASE = "com.sight.service.BeforePeriodicFileCleanup"
    }
}
