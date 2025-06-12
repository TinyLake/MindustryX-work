package mindustry.annotations.remote

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import mindustry.annotations.Annotations
import mindustry.annotations.Annotations.Loc
import mindustry.annotations.remote.CallGenerator.generate
import mindustry.annotations.util.TypeIOResolver

class RemoteProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        //get serializers
        //class serializers
        val serializer = TypeIOResolver.resolve(resolver)
        //last method ID used
        var lastMethodID = 0
        //find all elements with the Remote annotation
        //all elements with the Remote annotation
        val elements = resolver.getSymbolsWithAnnotation(Annotations.Remote::class.java.canonicalName).toList()
        //list of all method entries
        val methods = mutableListOf<MethodEntry>()

        val orderedElements = elements.sortedBy { it.toString() }

        orderedElements.forEach { ksAnnotated ->
            if (ksAnnotated !is KSClassDeclaration) return@forEach
            val annotation = ksAnnotated.annotations.first { it.annotationType.resolve().declaration.qualifiedName?.asString() == Annotations.Remote::class.java.canonicalName }

            if (!ksAnnotated.modifiers.contains(Modifier.JAVA_STATIC) || !ksAnnotated.modifiers.contains(Modifier.PUBLIC)) {
                logger.error("All @Remote methods must be public and static", ksAnnotated)
            }

            val targets = annotation.arguments.first { it.name?.asString() == "targets" }.value as Loc
            if (targets == Loc.none) {
                logger.error("A @Remote method's targets() cannot be equal to 'none'", ksAnnotated)
            }

            val packetName = capitalize(ksAnnotated.simpleName.asString()) + "CallPacket"
            val index = intArrayOf(1)

            while (methods.any { it.packetClassName == packetName + (if (index[0] == 1) "" else index[0]) }) {
                index[0]++
            }

            val method = MethodEntry(
                callLocation, ksAnnotated.simpleName.asString(), packetName + (if (index[0] == 1) "" else index[0]),
                targets, annotation.arguments.first { it.name?.asString() == "variants" }.value as Annotations.Variant,
                annotation.arguments.first { it.name?.asString() == "called" }.value as Loc, annotation.arguments.first { it.name?.asString() == "unreliable" }.value as Boolean,
                annotation.arguments.first { it.name?.asString() == "forward" }.value as Boolean, lastMethodID++,
                ksAnnotated as KSFunctionDeclaration, annotation.arguments.first { it.name?.asString() == "priority" }.value as Annotations.PacketPriority
            )

            methods.add(method)
        }

        generate(serializer, methods, codeGenerator, logger)

        return emptyList()
    }

    fun capitalize(s: String): String {
        val result = StringBuilder(s.length)

        for (i in 0 until s.length) {
            val c = s[i]
            if (c == '_' || c == '-') {
                result.append(" ")
            } else if (i == 0 || s[i - 1] == '_' || s[i - 1] == '-') {
                result.append(c.uppercaseChar())
            } else {
                result.append(c)
            }
        }

        return result.toString()
    }

    companion object {
        const val callLocation: String = "Call"
    }
}