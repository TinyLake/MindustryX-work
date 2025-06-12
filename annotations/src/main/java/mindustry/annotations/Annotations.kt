package mindustry.annotations

import kotlin.reflect.KClass

class Annotations {
    //region entity interfaces
    /** Indicates that a method overrides other methods.  */
    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Replace

    /** Indicates that a method should be final in all implementing classes.  */
    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Final

    /** Indicates that a field will be interpolated when synced.  */
    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.SOURCE)
    annotation class SyncField(
        /** If true, the field will be linearly interpolated. If false, it will be interpolated as an angle.  */
        val value: Boolean,
        /** If true, the field is clamped to 0-1.  */
        val clamped: Boolean = false
    )

    /** Indicates that a field will not be read from the server when syncing the local player state.  */
    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.SOURCE)
    annotation class SyncLocal

    /** Indicates that a field should not be synced to clients (but may still be non-transient)  */
    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.SOURCE)
    annotation class NoSync


    /** Indicates that a component field is imported from other components. This means it doesn't actually exist.  */
    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Import

    /** Indicates that a component field is read-only.  */
    @Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ReadOnly

    /** Indicates multiple inheritance on a component type.  */
    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Component(
        /** Whether to generate a base class for this components.
         * An entity cannot have two base classes, so only one component can have base be true.  */
        @JvmField
        val base: Boolean = false
    )

    /** Indicates that a method is implemented by the annotation processor.  */
    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class InternalImpl

    /** Indicates priority of a method in an entity. Methods with higher priority are done last.  */
    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class MethodPriority(val value: Float)

    /** Indicates that a component def is present on all entities.  */
    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.SOURCE)
    annotation class BaseComponent

    /** Creates a group that only examines entities that have all the components listed.  */
    @Retention(AnnotationRetention.SOURCE)
    annotation class GroupDef(vararg val value: KClass<*>, val collide: Boolean = false, val spatial: Boolean = false, val mapping: Boolean = false)

    /** Indicates an entity definition.  */
    @Retention(AnnotationRetention.SOURCE)
    annotation class EntityDef(
        /** List of component interfaces  */
        val value: Array<KClass<Any>>,
        /** Whether the class is final  */
        val isFinal: Boolean = true,
        /** If true, entities are recycled.  */
        val pooled: Boolean = false,
        /** Whether to serialize (makes the serialize method return this value).
         * If true, this entity is automatically put into save files.
         * If false, no serialization code is generated at all.  */
        val serialize: Boolean = true,
        /** Whether to generate IO code. This is for advanced usage only.  */
        val genio: Boolean = true,
        /** Whether I made a massive mistake by merging two different class branches  */
        val legacy: Boolean = false
    )

    /** Indicates an internal interface for entity components.  */
    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.SOURCE)
    annotation class EntityInterface

    //endregion
    //region misc. utility
    /** Automatically loads block regions annotated with this.  */
    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Load(
        /**
         * The region name to load. Variables can be used:
         * "@" -> block name
         * "@size" -> block size
         * "#" "#1" "#2" -> index number, for arrays
         */
        val value: String,
        /** 1D Array length, if applicable.   */
        val length: Int = 1,
        /** 2D array lengths.  */
        val lengths: IntArray = [],
        /** Fallback string used to replace "@" (the block name) if the region isn't found.  */
        val fallback: String = "error"
    )

    /** Registers a statement for auto serialization.  */
    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.SOURCE)
    annotation class RegisterStatement(val value: String)

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.SOURCE)
    annotation class StyleDefaults

    /** Indicates that a method should always call its super version.  */
    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class CallSuper

    /** Annotation that allows overriding CallSuper annotation. To be used on method that overrides method with CallSuper annotation from parent class.  */
    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class OverrideCallSuper

    //endregion
    //region struct
    /** Marks a class as a special value type struct. Class name must end in 'Struct'.  */
    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Struct

    /** Marks a field of a struct. Optional.  */
    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.SOURCE)
    annotation class StructField(
        /** Size of a struct field in bits. Not valid on booleans or floating point numbers.  */
        val value: Int
    )

    //endregion
    //region remote
    enum class PacketPriority {
        /** Does not get handled unless client is connected.  */
        low,

        /** Gets put in a queue and processed if not connected.  */
        normal,

        /** Gets handled immediately, regardless of connection status.  */
        high,
    }

    /** A set of two booleans, one specifying server and one specifying client.  */
    enum class Loc(
        /** If true, this method can be invoked ON clients FROM servers.  */
        @JvmField val isServer: Boolean,
        /** If true, this method can be invoked ON servers FROM clients.  */
        @JvmField val isClient: Boolean
    ) {
        /** Method can only be invoked on the client from the server.  */
        server(true, false),

        /** Method can only be invoked on the server from the client.  */
        client(false, true),

        /** Method can be invoked from anywhere  */
        both(true, true),

        /** Neither server nor client.  */
        none(false, false)
    }

    enum class Variant(@JvmField val isOne: Boolean, @JvmField val isAll: Boolean) {
        /** Method can only be invoked targeting one player.  */
        one(true, false),

        /** Method can only be invoked targeting all players.  */
        all(false, true),

        /** Method targets both one player and all players.  */
        both(true, true)
    }

    /** Marks a method as invokable remotely across a server/client connection.  */
    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Remote(
        /** Specifies the locations from which this method can be invoked.  */
        val targets: Loc = Loc.server,
        /** Specifies which methods are generated. Only affects server-to-client methods.  */
        val variants: Variant = Variant.all,
        /** The local locations where this method is called locally, when invoked.  */
        val called: Loc = Loc.none,
        /** Whether to forward this packet to all other clients upon receival. Client only.  */
        val forward: Boolean = false,
        /**
         * Whether the packet for this method is sent with UDP instead of TCP.
         * UDP is faster, but is prone to packet loss and duplication.
         */
        val unreliable: Boolean = false,
        /** Priority of this event.  */
        val priority: PacketPriority = PacketPriority.normal
    )

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.SOURCE)
    annotation class TypeIOHandler  //endregion
}
