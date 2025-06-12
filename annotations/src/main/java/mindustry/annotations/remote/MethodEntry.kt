package mindustry.annotations.remote

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import mindustry.annotations.Annotations
import mindustry.annotations.Annotations.Loc
import mindustry.annotations.Annotations.PacketPriority

/** Class that repesents a remote method to be constructed and put into a class.  */
class MethodEntry(
    /** Simple target class name.  */
    val className: String,
    /** Fully qualified target method to call.  */
    val targetMethod: String,
    /** Simple name of the generated packet class.  */
    @JvmField val packetClassName: String,
    /** Whether this method can be called on a client/server.  */
    val where: Loc,
    /**
     * Whether an additional 'one' and 'all' method variant is generated. At least one of these must be true.
     * Only applicable to client (server-invoked) methods.
     */
    val target: Annotations.Variant,
    /** Whether this method is called locally as well as remotely.  */
    val local: Loc,
    /** Whether this method is unreliable and uses UDP.  */
    val unreliable: Boolean,
    /** Whether to forward this method call to all other clients when a client invokes it. Server only.  */
    val forward: Boolean,
    /** Unique method ID.  */
    val id: Int,
    /** The element method associated with this entry.  */
    val element: KSFunctionDeclaration,
    /** The assigned packet priority. Only used in clients.  */
    val priority: PacketPriority
) {
    override fun hashCode(): Int {
        return targetMethod.hashCode()
    }
}
