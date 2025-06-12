package mindustry.annotations.impl

import arc.util.Strings
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import mindustry.annotations.Annotations
import mindustry.annotations.util.Utils.err
import mindustry.annotations.util.Utils.isPrimitive
import mindustry.annotations.util.Utils.packageName
import java.util.*
import kotlin.reflect.KClass


/**
 * Generates ""value types"" classes that are packed into integer primitives of the most aproppriate size.
 * It would be nice if Java didn't make crazy hacks like this necessary.
 */
class StructProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    @Throws(Exception::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val elements = resolver.getSymbolsWithAnnotation(Annotations.Struct::class.java.canonicalName)

        elements.forEach {
            val elem = it as KSClassDeclaration
            if (!elem.simpleName.asString().endsWith("Struct")) {
                logger.err("All classes annotated with @Struct must have their class names end in 'Struct'.", elem)
                return@forEach
            }

            val structName = elem.simpleName.asString().substring(0, elem.simpleName.asString().length - "Struct".length)
            val structParam = structName.lowercase(Locale.getDefault())

            val classBuilder = TypeSpec.objectBuilder(structName)

            try {
                val variables = elem.getAllProperties()
                val structSize = variables.map { a -> varSize(a) }.sum()
                val structTotalSize = (if (structSize <= 8) 8 else if (structSize <= 16) 16 else if (structSize <= 32) 32 else 64)

                if (variables.count() == 0) {
                    logger.err("making a struct with no fields is utterly pointles.", elem)
                    return@forEach
                }

                //obtain type which will be stored
                val structType = typeForSize(structSize)

                //[constructor] get(fields...) : structType
                val constructor = FunSpec.builder("get")
                    .addModifiers(KModifier.PUBLIC)
                    .returns(structType)

                val cons = StringBuilder()
                val doc = StringBuilder()
                doc.append("Bits used: ").append(structSize).append(" / ").append(structTotalSize).append("\n")

                var offset = 0
                for (`var` in variables) {
                    val size = varSize(`var`)
                    val varType = `var`.type.toTypeName()
                    val varName = `var`.simpleName.asString()
                    val isBool = `var`.type.resolve().toClassName() === BOOLEAN

                    //add val param to constructor
                    constructor.addParameter(varName, varType)

                    //[get] field(structType) : fieldType
                    val getter = FunSpec.builder(`var`.simpleName.asString())
                        .addModifiers(KModifier.PUBLIC)
                        .returns(varType)
                        .addParameter(structParam, structType)
                    //[set] field(structType, fieldType) : structType
                    val setter = FunSpec.builder(`var`.simpleName.asString())
                        .addModifiers(KModifier.PUBLIC)
                        .returns(structType)
                        .addParameter(structParam, structType).addParameter("value", varType)

                    //field for offset
                    classBuilder.addProperty(
                        PropertySpec.builder("bitMask" + Strings.capitalize(varName), structType)
                            .apply {
                                if (!isBool)
                                    initializer("(%L as %T)", bitString(offset, size, structTotalSize), structType)
                                else
                                    initializer("((1L shl %L) as %T)", offset, structType)
                            }.build()
                    )

                    //[getter]
                    if (isBool) {
                        //bools: single bit, is simplified
                        getter.addStatement("return (%L and (1L shl %L)) != 0", structParam, offset)
                    } else if (`var`.type.resolve().toClassName() === FLOAT) {
                        //floats: need conversion
                        getter.addStatement("return Float.intBitsToFloat((((%L ushr %L) and %L)) as Int)", structParam, offset, bitString(size, structTotalSize))
                    } else {
                        //bytes, shorts, chars, ints
                        getter.addStatement("return (((%L ushr %L) and %L) as %T)", structParam, offset, bitString(size, structTotalSize), varType)
                    }

                    //[setter] + [constructor building]
                    if (isBool) {
                        cons.append(" or (").append(varName).append(" ? ").append("1L shl ").append(offset).append("L : 0)")

                        //bools: single bit, needs special case to clear things
                        setter.beginControlFlow("if(value)")
                        setter.addStatement("return (((%L and (1L shl %LL).inv()) as %T) or (1L shl %LL))", structParam, offset, structType, offset)
                        setter.nextControlFlow("else")
                        setter.addStatement("return (((%L and (1L shl %LL).inv())) as %T)", structParam, offset, structType)
                        setter.endControlFlow()
                    } else if (`var`.type.resolve().toClassName() === FLOAT) {
                        cons.append(" or (").append("(").append(structType).append(")").append("Float.floatToIntBits(").append(varName).append(") shl ").append(offset).append("L)")

                        //floats: need conversion
                        setter.addStatement("return (((%L and (%L.inv())) as %T) or ((Float.floatToIntBits(value) as %T) shl %L))", structParam, bitString(offset, size, structTotalSize), structType, structType, offset)
                    } else {
                        cons.append(" or ((").append(varName).append(" as ").append(structType.asTypeName().simpleName).append(" shl ").append(offset).append(")").append(" and ").append(bitString(offset, size, structTotalSize)).append(")")

                        //bytes, shorts, chars, ints
                        setter.addStatement("return (((%L and (%L.inv())) as %T) or ((value as %T) shl %L))", structParam, bitString(offset, size, structTotalSize), structType, structType, offset)
                    }

                    doc.append("<br>  ").append(varName).append(" [").append(offset).append("..").append(size + offset).append("]\n")

                    //add finished methods
                    classBuilder.addFunction(getter.build())
                    classBuilder.addFunction(setter.build())

                    offset += size
                }

                classBuilder.addKdoc(doc.toString())

                //add constructor final statement + add to class and build
                constructor.addStatement("return (%L as %T)", cons.substring(3), structType)
                classBuilder.addFunction(constructor.build())

                FileSpec.builder(packageName, classBuilder.build().name!!).addType(classBuilder.build()).build().writeTo(codeGenerator, true)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                logger.err(e.message!!, elem)
            }
        }
        return emptyList()
    }

    companion object {
        fun bitString(offset: Int, size: Int, totalSize: Int): String {
            val builder = StringBuilder()
            for (i in 0 until offset) builder.append('0')
            for (i in 0 until size) builder.append('1')
            for (i in 0 until totalSize - size - offset) builder.append('0')
            return "0b" + builder.reverse().toString()
        }

        fun bitString(size: Int, totalSize: Int) = bitString(0, size, totalSize)

        @Throws(IllegalArgumentException::class)
        fun varSize(`var`: KSPropertyDeclaration): Int {
            require(`var`.type.resolve().isPrimitive()) { "All struct fields must be primitives: $`var`: ${`var`.type}" }

            val an = `var`.annotation(Annotations.StructField::class)
            require(!(`var`.type.resolve().toClassName() == BOOLEAN && an != null && (an.arguments[0].value as Int) != 1)) { "Booleans can only be one bit long... why would you do this?" }

            require(!(`var`.type.resolve().toClassName() == FLOAT && an != null && (an.arguments[0].value as Int) != 32)) { "Float size can't be changed. Very sad." }

            return if (an != null) {
                an.arguments[0].value as Int
            } else {
                typeSize(`var`.type.resolve().toClassName())
            }
        }

        //From chatgpt, not checked
        inline fun <reified T : Annotation> KSPropertyDeclaration.annotation(annotationClass: KClass<T>): KSAnnotation? {
            // 获取注解的全名
            val annotationQualifiedName = annotationClass.qualifiedName

            // 查找与注解类型匹配的注解
            return annotations.find {
                it.shortName.asString() == annotationQualifiedName?.substringAfterLast('.') &&
                        it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationQualifiedName
            }
        }

        //From chatgpt, not checked
        inline fun <reified T : Annotation> KSClassDeclaration.annotation(annotationClass: KClass<T>): KSAnnotation? {
            // 获取注解的全名
            val annotationQualifiedName = annotationClass.qualifiedName

            // 查找与注解类型匹配的注解
            return annotations.find {
                it.shortName.asString() == annotationQualifiedName?.substringAfterLast('.') &&
                        it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationQualifiedName
            }
        }

        @Throws(IllegalArgumentException::class)
        fun typeForSize(size: Int): KClass<*> {
            if (size <= 8) {
                return Byte::class
            } else if (size <= 16) {
                return Short::class
            } else if (size <= 32) {
                return Int::class
            } else if (size <= 64) {
                return Long::class
            }
            throw IllegalArgumentException("Too many fields, must fit in 64 bits. Curent size: $size")
        }

        /** returns a type's element size in bits.  */
        @Throws(IllegalArgumentException::class)
        fun typeSize(kind: ClassName): Int {
            return when (kind) {
                BOOLEAN -> 1
                BYTE -> 8
                SHORT -> 16
                FLOAT, CHAR, INT -> 32
                else -> throw IllegalArgumentException("Invalid type kind: $kind. Note that doubles and longs are not supported.")
            }
        }
    }
}