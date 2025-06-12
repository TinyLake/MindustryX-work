package mindustry.annotations.util

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ksp.toTypeName
import mindustry.annotations.Annotations.TypeIOHandler

/**
 * This class finds reader and writer methods.
 */
object TypeIOResolver {
    /**
     * Finds all class serializers for all types and returns them. Logs errors when necessary.
     * Maps fully qualified class names to their serializers.
     */
    fun resolve(resolver: Resolver): ClassSerializer {
        val out = ClassSerializer(mutableMapOf(), mutableMapOf(), mutableMapOf(), mutableMapOf())
        resolver.getSymbolsWithAnnotation(TypeIOHandler::class.java.canonicalName)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { declaration ->
                //look at all TypeIOHandler methods
                declaration.getDeclaredFunctions()
                    .filter { it.simpleName.asString() !in setOf("<init>", "<clinit>") }
                    .forEach { method ->
                        if (method.modifiers.contains(Modifier.PUBLIC) && method.modifiers.contains(Modifier.JAVA_STATIC)) {
                            val params = method.parameters
                            //2 params, second one is declaration, first is writer
                            if (params.size == 2 && params.first().type.toTypeName().toString() == "arc.util.io.Writes") {
                                //Net suffix indicates that this should only be used for sync operations
                                val targetMap = if (method.simpleName.asString().endsWith("Net")) out.netWriters else out.writers

                                //logger.err(method.simpleName.asString())
                                //logger.err(params[1].type.toString())
                                //TODO because the type is not resolved, the type name is not correct

                                targetMap[fix(params[1].type.toTypeName().toString())] = declaration.qualifiedName?.asString() + "." + method.simpleName.asString()
                            } else if (params.size == 1 && params.first().type.toTypeName().toString() == "arc.util.io.Reads" && method.returnType?.toTypeName().toString() != "void") {
                                //1 param, one is reader, returns declaration
                                out.readers[fix(method.returnType.toString())] = declaration.qualifiedName?.asString() + "." + method.simpleName.asString()
                            } else if (params.size == 2 && params.first().type.toTypeName().toString() == "arc.util.io.Reads" && method.returnType?.toTypeName().toString() == "void" && method.returnType == method.parameters[1].type) {
                                //2 params, one is reader, other is declaration, returns declaration - these are made to reduce garbage allocated
                                out.mutatorReaders[fix(method.returnType.toString())] = declaration.qualifiedName?.asString() + "." + method.simpleName.asString()
                            }
                        }
                    }
            }

        return out
    }

    /** makes sure type names don't contain 'gen'  */
    private fun fix(str: String): String {
        return str.replace("mindustry.gen", "")
    }

    /** Information about read/write methods for class types.  */
    class ClassSerializer(val writers: MutableMap<String?, String?>, val readers: MutableMap<String?, String?>, val mutatorReaders: MutableMap<String?, String?>, val netWriters: MutableMap<String?, String?>) {
        fun getNetWriter(type: String?, fallback: String?): String? {
            return netWriters[type] ?: (writers[type] ?: fallback.also { writers[type] = fallback }).also { netWriters[type] = fallback }
        }
    }
}
