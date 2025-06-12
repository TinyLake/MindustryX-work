package mindustry.annotations.misc

import arc.func.Prov
import arc.struct.Seq
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import mindustry.annotations.Annotations.RegisterStatement
import mindustry.annotations.impl.StructProcessor.Companion.annotation
import mindustry.annotations.util.Utils.err
import mindustry.annotations.util.Utils.isPrimitive
import mindustry.annotations.util.Utils.packageName
import mindustry.annotations.util.Utils.typeName
import javax.annotation.processing.SupportedAnnotationTypes

@SupportedAnnotationTypes("mindustry.annotations.Annotations.RegisterStatement")
class LogicStatementProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    @Throws(Exception::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val type = TypeSpec.objectBuilder("LogicIO")
            .addModifiers(KModifier.PUBLIC)

        val writer = FunSpec.builder("write")
            .addModifiers(KModifier.PUBLIC)
            .addParameter("obj", Any::class)
            .addParameter("out", StringBuilder::class)

        val reader = FunSpec.builder("read")
            .addModifiers(KModifier.PUBLIC)
            .returns(typeName("mindustry.logic.LStatement").copy(true))
            .addParameter("tokens", Array::class.parameterizedBy(String::class))
            .addParameter("length", Int::class)

        val types = Seq.with(resolver.getSymbolsWithAnnotation(RegisterStatement::class.java.canonicalName).toList())

        type.addProperty(PropertySpec.builder(
            "allStatements",
            Seq::class.asTypeName().parameterizedBy(Prov::class.asTypeName().parameterizedBy(typeName("mindustry.logic.LStatement"))),
            KModifier.PUBLIC
        )
            .initializer("Seq.with(" + types.toString(", ") { "\narc.func.Prov { LStatements.$it() }" } + "\n)").build())

        var beganWrite = false
        var beganRead = false

        for (c in types) {
            val name = (c as KSClassDeclaration).annotation(RegisterStatement::class)!!.arguments[0]

            if (beganWrite) {
                writer.nextControlFlow("else if(obj.javaClass == %T::class.java)", c.toClassName())
            } else {
                writer.beginControlFlow("if(obj.javaClass == %T::class.java)", c.toClassName())
                beganWrite = true
            }

            //write the name & individual fields
            writer.addStatement("`out`.append(%S)", name)

            val fields = c.getDeclaredProperties().toMutableList()
            c.superTypes
                .mapNotNull { resolver.getClassDeclarationByName(it.resolve().declaration.qualifiedName!!) }
                .filter { it.classKind == ClassKind.CLASS }.toList().firstOrNull()?.getDeclaredProperties()?.also { fields.addAll(it) }


            val readSt = "if(tokens[0].equals(%S))"
            if (beganRead) {
                reader.nextControlFlow("else $readSt", name)
            } else {
                reader.beginControlFlow(readSt, name)
                beganRead = true
            }

            reader.addStatement("val result: %T = %T()", c.toClassName(), c.toClassName())

            var index = 0

            fields.forEach { field ->
                if (field.modifiers.contains(com.google.devtools.ksp.symbol.Modifier.JAVA_TRANSIENT)) return@forEach
                if (field.modifiers.contains(com.google.devtools.ksp.symbol.Modifier.JAVA_STATIC)) return@forEach

                writer.addStatement("out.append(\" \")")
                writer.addStatement(
                    "out.append((obj as %T).%L%L)", c.toClassName(), field.toString(),
                    if ((field.parentDeclaration as KSClassDeclaration)
                            .getAllSuperTypes().any { it.toString().contains("java.lang.Enum") }
                    ) ".name()" else ""
                )

                //reading primitives, strings and enums is supported; nothing else is
                if (field.toString().contains("StatementElem")) {
                    logger.err(field.modifiers.toList().toString())
                }
                reader.addStatement(
                    "if(length > %L) result.%L = %L(tokens[%L]%L)",
                    index + 1,
                    field,
                    if (field.type.toTypeName().toString().lowercase().contains("string")) ""
                    else if (field.type.toTypeName().toString().lowercase().contains("kotlin")) ""
                    else (
                            if (isPrimitive(field.type.toTypeName().toString())) field.type.toTypeName().toString()
                            else
                                field.type.toTypeName().toString()
                            ) + ".valueOf",  //if it's not a string, it must have a valueOf method
                    index + 1,
                    if (field.type.toTypeName().toString().lowercase().contains("kotlin")) {
                        when (field.type.toTypeName().toString()) {
                            "kotlin.Int" -> ".toInt()"
                            "kotlin.Float" -> ".toFloat()"
                            "kotlin.Double" -> ".toDouble()"
                            "kotlin.Long" -> ".toLong()"
                            "kotlin.Short" -> ".toShort()"
                            "kotlin.Byte" -> ".toByte()"
                            "kotlin.Boolean" -> ".toBoolean()"
                            else -> ""
                        }
                    } else ""
                )

                index++
            }

            reader.addStatement("result.afterRead()")
            reader.addStatement("return result")
        }

        reader.endControlFlow()
        writer.endControlFlow()

        reader.addStatement("return null")

        type.addFunction(writer.build())
        type.addFunction(reader.build())

        FileSpec.builder(packageName, type.build().name.toString())
            .addType(type.build())
            .build()
            .writeTo(codeGenerator, true)

        return emptyList()
    }
}
