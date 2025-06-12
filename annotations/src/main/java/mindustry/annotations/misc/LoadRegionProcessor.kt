package mindustry.annotations.misc

import arc.Core
import arc.func.Prov
import arc.graphics.g2d.TextureRegion
import arc.struct.ObjectMap
import arc.struct.Seq
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import mindustry.annotations.Annotations
import mindustry.annotations.impl.StructProcessor.Companion.annotation
import mindustry.annotations.util.Utils.err
import mindustry.annotations.util.Utils.packageName
import mindustry.annotations.util.Utils.typeName

class LoadRegionProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    @Throws(Exception::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val regionClass = TypeSpec.objectBuilder("ContentRegions")
            .addModifiers(KModifier.PUBLIC)
        val method = FunSpec.builder("loadRegions")
            .addParameter("content", typeName("mindustry.ctype.MappableContent"))
            .addModifiers(KModifier.PUBLIC)

        val fieldMap = ObjectMap<TypeName, Seq<KSPropertyDeclaration>>()

        resolver.getSymbolsWithAnnotation(Annotations.Load::class.java.canonicalName).forEach { field ->
            if (!(field as KSPropertyDeclaration).isPublic()) {
                logger.err("@LoadRegion field must be public", field as KSNode)
            }

            fieldMap[typeName("${field.packageName.asString()}.${field.parent!!}"), Prov { Seq() }].add(field)
        }

        val entries = Seq.with(fieldMap.keys())
        entries.sortComparing { it.toString() }

        entries.forEach { type ->
            val fields = fieldMap[type]
            fields.sortComparing { s -> s.simpleName.asString() }
            method.beginControlFlow("if(content is %L)", type)

            for (field in fields) {
                val an = field.annotation(Annotations.Load::class)!!
                //get # of array dimensions
                val dims = count(field.type.toTypeName().toString(), "Array")
                val doFallback = an.arguments[3].value.toString() != "error"
                val fallbackString = if (doFallback) ", " + parse(an.arguments[3].value.toString()) else ""

                //not an array
                if (dims == 0) {
                    method.addStatement("content.%L = %T.atlas.find(%L%L)", field.toString(), Core::class.java, parse(an.arguments[0].value.toString()), fallbackString)
                } else {
                    //is an array, create length string
                    var lengths = an.arguments.find { it.name!!.asString().contains("lengths") }!!.value as ArrayList<Int>
                    if (lengths.isEmpty()) lengths = arrayListOf(an.arguments[1].value as Int)

                    if (dims != lengths.size) {
                        logger.err("Length dimensions must match array dimensions: " + dims + " != " + lengths.size, field)
                    }

                    method.addStatement("content.%L = ", field.toString())
                    for (value in lengths) method.beginControlFlow("Array(%L)", value)
                    method.addStatement("%T()", TextureRegion::class.java)
                    for (value in lengths) method.endControlFlow()

                    for (i in 0 until dims) {
                        method.beginControlFlow("for(INDEX%L in 0 until %L)", i, lengths[i])
                    }

                    val indexString = StringBuilder()
                    for (i in 0 until dims) {
                        indexString.append("[INDEX").append(i).append("]")
                    }

                    method.addStatement("content.%L%L = %T.atlas.find(%L%L)", field.toString(), indexString.toString(), Core::class.java, parse(an.arguments[0].value.toString()), fallbackString)

                    for (i in 0 until dims) {
                        method.endControlFlow()
                    }
                }
            }

            method.endControlFlow()
        }

        regionClass.addFunction(method.build())

        FileSpec.builder(packageName, regionClass.build().name!!).addType(regionClass.build()).build().writeTo(codeGenerator, true)

        return emptyList()
    }

    private fun parse(value: String): String {
        var value = value
        value = '"'.toString() + value + '"'
        value = value.replace("@size", "\" + ((mindustry.world.Block)content).size + \"")
        value = value.replace("@", "\" + content.name + \"")
        value = value.replace("#1", "\" + INDEX0 + \"")
        value = value.replace("#2", "\" + INDEX1 + \"")
        value = value.replace("#", "\" + INDEX0 + \"")
        return value
    }

    companion object {
        private fun count(str: String, substring: String): Int {
            var lastIndex = 0
            var count = 0

            while (lastIndex != -1) {
                lastIndex = str.indexOf(substring, lastIndex)

                if (lastIndex != -1) {
                    count++
                    lastIndex += substring.length
                }
            }
            return count
        }
    }
}
