package mindustry.annotations.util

import arc.util.Log
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName

object Utils {
    const val packageName: String = "mindustry.gen"
    var logger: KSPLogger? = null

    @JvmStatic
    fun typeName(pack: String?, simple: String?): TypeName {
        return ClassName.bestGuess("$pack.$simple")
    }

    @JvmStatic
    fun typeName(name: String): TypeName {
        if (!name.contains(".")) return ClassName.bestGuess("$packageName.$name")

        val pack = name.substring(0, name.lastIndexOf("."))
        val simple = name.substring(name.lastIndexOf(".") + 1)
        return ClassName.bestGuess("$pack.$simple")
    }

    @JvmStatic
    fun typeName(c: Class<*>): TypeName {
        return c.asTypeName()
    }

    @JvmStatic
    fun isPrimitive(type: String): Boolean {
        return type == "boolean" || type == "byte" || type == "short" || type == "int" || type == "long" || type == "float" || type == "double" || type == "char"
    }

    @JvmStatic
    fun KSType.isPrimitive(): Boolean {
        return this.toTypeName().toString().toLowerCase().contains("boolean")
                || this.toTypeName().toString().toLowerCase().contains("byte")
                || this.toTypeName().toString().toLowerCase().contains("short")
                || this.toTypeName().toString().toLowerCase().contains("int")
                || this.toTypeName().toString().toLowerCase().contains("long")
                || this.toTypeName().toString().toLowerCase().contains("float")
                || this.toTypeName().toString().toLowerCase().contains("double")
                || this.toTypeName().toString().toLowerCase().contains("char")
    }

    @JvmStatic
    fun KSPLogger.err(message: String) {
        this.error(message)
    }

    @JvmStatic
    fun KSPLogger.err(message: String, elem: KSNode) {
        this.error(message, elem)
    }

    @JvmStatic
    fun KSPLogger.err(message: String, elem: KSDeclaration) {
        this.err(message, elem.parent!!)
    }
}