package mindustry.annotations.remote

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType

object SerializerResolver {
    fun locate(ksDeclaration: KSDeclaration, ksType: KSType, write: Boolean): String? {
        //generic type
        if (((ksType.toString() == "T") && ksDeclaration.typeParameters[0].bounds.any { isEntity(it.resolve().toString()) } ) ||
            isEntity(ksType.toString())
        ) {
            return if (write) "mindustry.io.TypeIO.writeEntity" else "mindustry.io.TypeIO.readEntity"
        }
        return null
    }

    private fun isEntity(typeString: String): Boolean {
        return !typeString.contains(".") || typeString.startsWith("mindustry.gen.") && !typeString.startsWith("byte")
    }
}
