package mindustry.annotations.remote

import arc.util.io.Reads
import arc.util.io.Writes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import com.squareup.kotlinpoet.ksp.writeTo
import mindustry.annotations.Annotations.Loc
import mindustry.annotations.Annotations.PacketPriority
import mindustry.annotations.util.TypeIOResolver
import mindustry.annotations.util.Utils.err
import mindustry.annotations.util.Utils.isPrimitive
import mindustry.annotations.util.Utils.packageName
import mindustry.annotations.util.Utils.typeName
import java.io.IOException

/** Generates code for writing remote invoke packets on the client and server.  */
object CallGenerator {
    private lateinit var codeGenerator: CodeGenerator
    private lateinit var logger: KSPLogger

    /** Generates all classes in this list.  */
    @JvmStatic
    @Throws(IOException::class)
    fun generate(
        serializer: TypeIOResolver.ClassSerializer,
        methods: MutableList<MethodEntry>,
        codeGenerator: CodeGenerator,
        logger: KSPLogger
    ) {
        this.codeGenerator = codeGenerator
        this.logger = logger

        //创建生成器
        val callBuilder = TypeSpec.objectBuilder(RemoteProcessor.callLocation).addModifiers(KModifier.PUBLIC)

        val register = FunSpec.builder("registerPackets")
            .addModifiers(KModifier.PUBLIC)

        //遍历此类中的每个方法条目
        methods.forEach { ent ->
            //数据包类型的构建器
            val packet = TypeSpec.classBuilder(ent.packetClassName)
                .addModifiers(KModifier.PUBLIC)

            //稍后要反序列化的临时数据
            packet.addProperty(PropertySpec.builder("DATA", ByteArray::class, KModifier.PRIVATE).initializer("NODATA").build())

            packet.superclass(typeName("mindustry.net.Packet"))

            //返回正确的优先级
            if (ent.priority != PacketPriority.normal) {
                packet.addFunction(
                    FunSpec.builder("getPriority")
                        .addModifiers(KModifier.PUBLIC)
                        .addAnnotation(Override::class)
                        .returns(Int::class)
                        .addStatement("return %L", ent.priority.ordinal)
                        .build()
                )
            }

            //实现读写方法
            makeWriter(packet, ent, serializer)
            makeReader(packet, ent, serializer)

            //生成处理程序
            if (ent.where.isClient) {
                packet.addFunction(writeHandleMethod(ent, false))
            }

            if (ent.where.isServer) {
                packet.addFunction(writeHandleMethod(ent, true))
            }

            //注册包
            register.addStatement("mindustry.net.Net.registerPacket(%L.%L::new)", packageName, ent.packetClassName)

            //向类型添加字段
            val params = ent.element.parameters
            for (i in params.indices) {
                if (!ent.where.isServer && i == 0) {
                    continue
                }

                val param = params[i]
                packet.addProperty(param.name!!.asString(), param.type.toTypeName(), KModifier.PUBLIC)
            }

            //编写“向所有玩家发送事件”变体：始终发生在客户端上，但仅当在服务器方法上启用“all”时才会发生
            if (ent.where.isClient || ent.target.isAll) {
                writeCallMethod(callBuilder, ent, true, false)
            }

            //编写“向一个玩家发送事件”变体，该变体仅适用于服务器
            if (ent.where.isServer && ent.target.isOne) {
                writeCallMethod(callBuilder, ent, false, false)
            }

            //write the forwarded method version
            if (ent.where.isServer && ent.forward) {
                writeCallMethod(callBuilder, ent, true, true)
            }

            //写入完成的数据包类
            FileSpec.builder(packageName = packageName, fileName = packet.build().name!!)
                .addType(packet.build())
                .build().writeTo(codeGenerator, true)
        }

        callBuilder.addFunction(register.build())

        //生成并写入生成的类
        val spec = callBuilder.build()
        logger.err("writing")
        FileSpec.builder(packageName = packageName, fileName = spec.name!!)
            .addType(spec)
            .build().writeTo(codeGenerator, true)
    }

    private fun makeWriter(typespec: TypeSpec.Builder, ent: MethodEntry, serializer: TypeIOResolver.ClassSerializer) {
        val builder = FunSpec.builder("write")
            .addParameter("WRITE", Writes::class)
            .addModifiers(KModifier.PUBLIC).addAnnotation(Override::class)
        val params = ent.element.parameters

        for (i in 0 until params.size) {
            //first argument is skipped as it is always the player caller
            if (!ent.where.isServer && i == 0) {
                continue
            }

            val `var` = params[i]

            //参数名称
            val varName = `var`.name!!.asString()
            //参数类型的名称
            val typeName = `var`.type.toTypeName().toString()
            //特殊情况：方法可以从任何地方调用到任何地方
            //因此，只有在服务器写入数据时才写入玩家，因为客户端是唯一读取玩家的人
            val writePlayerSkipCheck = ent.where == Loc.both && i == 0

            if (writePlayerSkipCheck) { //write begin check
                builder.beginControlFlow("if(mindustry.Vars.net.server())")
            }

            if (isPrimitive(typeName)) { //check if it's a primitive, and if so write it
                builder.addStatement("WRITE.%L(%L)", if (typeName == "boolean") "bool" else typeName[0].toString() + "", varName)
            } else {
                //else, try and find a serializer
                val ser = serializer.getNetWriter(typeName.replace("mindustry.gen.", ""), SerializerResolver.locate(ent.element.parentDeclaration!!, `var`.type.resolve(), true))

                if (ser == null) { //make sure a serializer exists!
                    logger.err("No method to write class type: '$typeName'", `var`)
                }

                //add statement for writing it
                builder.addStatement("$ser(WRITE, $varName)")
            }

            if (writePlayerSkipCheck) { //write end check
                builder.endControlFlow()
            }
        }

        typespec.addFunction(builder.build())
    }

    private fun makeReader(typespec: TypeSpec.Builder, ent: MethodEntry, serializer: TypeIOResolver.ClassSerializer) {
        val readbuilder = FunSpec.builder("read")
            .addParameter("READ", Reads::class)
            .addParameter("LENGTH", Int::class)
            .addModifiers(KModifier.PUBLIC).addAnnotation(Override::class)

        //read only into temporary data buffer
        readbuilder.addStatement("DATA = READ.b(LENGTH)")

        typespec.addFunction(readbuilder.build())

        val builder = FunSpec.builder("handled")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Override::class)

        //make sure data is present, begin reading it if so
        builder.addStatement("BAIS.setBytes(DATA)")

        val params = ent.element.parameters

        //go through each parameter
        for (i in params.indices) {
            val `var` = params[i]

            //first argument is skipped as it is always the player caller
            if (!ent.where.isServer && i == 0) {
                continue
            }

            //special case: method can be called from anywhere to anywhere
            //thus, only read the player when the CLIENT is receiving data, since the client is the only one who cares about the player anyway
            val writePlayerSkipCheck = ent.where == Loc.both && i == 0

            if (writePlayerSkipCheck) { //write begin check
                builder.beginControlFlow("if(mindustry.Vars.net.client())")
            }

            //full type name of parameter
            val typeName = `var`.type.toTypeName().toString()
            //name of parameter
            val varName = `var`.name!!.asString()
            //capitalized version of type name for reading primitives
            val pname = if (typeName == "boolean") "bool" else typeName[0].toString()

            //write primitives automatically
            if (isPrimitive(typeName)) {
                builder.addStatement("%L = READ.%L()", varName, pname)
            } else {
                //else, try and find a serializer
                val ser = serializer.readers[typeName.replace("mindustry.gen.", "")] ?: (SerializerResolver.locate(ent.element.parentDeclaration!!, `var`.type.resolve(), false)).also { serializer.readers[typeName.replace("mindustry.gen.", "")] = it }

                if (ser == null) { //make sure a serializer exists!
                    logger.err("No read method to read class type '" + typeName + "' in method " + ent.targetMethod + "; " + serializer.readers, `var`)
                }

                //add statement for reading it
                builder.addStatement("%L = %L(READ)", varName, ser!!)
            }

            if (writePlayerSkipCheck) { //write end check
                builder.endControlFlow()
            }
        }

        typespec.addFunction(builder.build())
    }

    /** Creates a specific variant for a method entry.  */
    private fun writeCallMethod(classBuilder: TypeSpec.Builder, ent: MethodEntry, toAll: Boolean, forwarded: Boolean) {
        val elem = ent.element
        val params = elem.parameters

        //create builder
        val method = FunSpec.builder(elem.simpleName.asString() + (if (forwarded) "__forward" else "")) //add except suffix when forwarding
            .returns(Void.TYPE)

        //forwarded methods aren't intended for use, and are not public
        if (!forwarded) {
            method.addModifiers(KModifier.PUBLIC)
        }

        //validate client methods to make sure
        if (ent.where.isClient) {
            if (params.isEmpty()) {
                logger.err("Client invoke methods must have a first parameter of type Player", elem)
                return
            }

            if (!params[0].type.toTypeName().toString().contains("Player")) {
                logger.err("Client invoke methods should have a first parameter of type Player", elem)
                return
            }
        }

        //if toAll is false, it's a 'send to one player' variant, so add the player as a parameter
        if (!toAll) {
            method.addParameter("playerConnection", ClassName.bestGuess("mindustry.net.NetConnection"))
        }

        //add sender to ignore
        if (forwarded) {
            method.addParameter("exceptConnection", ClassName.bestGuess("mindustry.net.NetConnection"))
        }

        //call local method if applicable, shouldn't happen when forwarding method as that already happens by default
        if (!forwarded && ent.local != Loc.none) {
            //add in local checks
            if (ent.local != Loc.both) {
                method.beginControlFlow("if(" + getCheckString(ent.local) + " || !mindustry.Vars.net.active())")
            }

            //concatenate parameters
            var index = 0
            val results = StringBuilder()
            for (`var` in params) {
                //special case: calling local-only methods uses the local player
                if (index == 0 && ent.where == Loc.client) {
                    results.append("mindustry.Vars.player")
                } else {
                    results.append(`var`.name!!.asString())
                }
                if (index != params.size - 1) results.append(", ")
                index++
            }

            //add the statement to call it
            method.addStatement(
                "%N." + elem.simpleName + "(" + results + ")",
                (elem.parent as KSClassDeclaration).qualifiedName!!.asString()
            )

            if (ent.local != Loc.both) {
                method.endControlFlow()
            }
        }

        //start control flow to check if it's actually client/server so no netcode is called
        method.beginControlFlow("if(" + getCheckString(ent.where) + ")")

        //add statement to create packet from pool
        method.addStatement("$1T packet = new $1T()", typeName("mindustry.gen." + ent.packetClassName))

        method.addTypeVariables(elem.typeParameters.map { element: KSTypeParameter -> element.toTypeVariableName() })

        for (i in 0 until params.size) {
            //first argument is skipped as it is always the player caller
            if ((!ent.where.isServer) && i == 0) {
                continue
            }

            val `var` = params[i]

            method.addParameter(`var`.name!!.asString(), `var`.type.toTypeName())

            //name of parameter
            val varName = `var`.name!!.asString()
            //special case: method can be called from anywhere to anywhere
            //thus, only write the player when the SERVER is writing data, since the client is the only one who reads it
            val writePlayerSkipCheck = ent.where == Loc.both && i == 0

            if (writePlayerSkipCheck) { //write begin check
                method.beginControlFlow("if(mindustry.Vars.net.server())")
            }

            method.addStatement("packet.%L = %L", varName, varName)

            if (writePlayerSkipCheck) { //write end check
                method.endControlFlow()
            }
        }

        val sendString = if (forwarded) { //forward packet
            if (!ent.local.isClient) { //if the client doesn't get it called locally, forward it back after validation
                "mindustry.Vars.net.send("
            } else {
                "mindustry.Vars.net.sendExcept(exceptConnection, "
            }
        } else if (toAll) { //send to all players / to server
            "mindustry.Vars.net.send("
        } else { //send to specific client from server
            "playerConnection.send("
        }

        //send the actual packet
        method.addStatement(sendString + "packet, " + (!ent.unreliable) + ")")


        //end check for server/client
        method.endControlFlow()

        //add method to class, finally
        classBuilder.addFunction(method.build())
    }

    private fun getCheckString(loc: Loc): String {
        return if (loc.isClient && loc.isServer) "mindustry.Vars.net.server() || mindustry.Vars.net.client()" else if (loc.isClient) "mindustry.Vars.net.client()" else if (loc.isServer) "mindustry.Vars.net.server()" else "false"
    }

    /** Generates handleServer / handleClient methods.  */
    private fun writeHandleMethod(ent: MethodEntry, isClient: Boolean): FunSpec {
        //create main method builder

        val builder = FunSpec.builder(if (isClient) "handleClient" else "handleServer")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Override::class)
            .returns(Void.TYPE)

        val elem = ent.element
        val params = elem.parameters

        if (!isClient) {
            //add player parameter
            builder.addParameter("con", typeName("mindustry.net.NetConnection"))

            //skip if player is invalid
            builder.beginControlFlow("if(con.player == null || con.kicked)")
            builder.addStatement("return")
            builder.endControlFlow()

            //make sure to use the actual player who sent the packet
            builder.addStatement("mindustry.gen.Player player = con.player")
        }

        //execute the relevant method before the forward
        //if it throws a ValidateException, the method won't be forwarded
        builder.addStatement("%N." + elem.simpleName.asString() + "(" + params.joinToString(", ") { it.name!!.asString() } + ")", (elem.parent as KSClassDeclaration).qualifiedName!!.asString())

        //call forwarded method, don't forward on the client reader
        if (ent.forward && ent.where.isServer && !isClient) {
            //call forwarded method
            builder.addStatement("%L.%L.%L__forward(con, %L)", packageName, ent.className, elem.simpleName, params.joinToString(", ") { it.name!!.asString() })
        }

        return builder.build()
    }
}
