package mindustryX

import javassist.ClassPool
import javassist.CtClass
import javassist.bytecode.Bytecode
import javassist.bytecode.Descriptor
import java.lang.reflect.Modifier
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    java
}

@CacheableTransform
abstract class PatchArc : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputArtifact: Provider<FileSystemLocation>

    private val genDir = File("build/gen/patchedArc")
    private val transform = mapOf<String, CtClass.() -> Unit>(
        "arc.util.Http\$HttpRequest" to clz@{
            getDeclaredMethod("block").apply {
                val code = Bytecode(methodInfo.constPool)
                val desc = Descriptor.ofMethod(CtClass.voidType, arrayOf(this@clz))
                code.addAload(0)
                code.addInvokestatic("mindustryX.Hooks", "onHttp", desc)
                methodInfo.codeAttribute.iterator().insertEx(code.get())
                methodInfo.rebuildStackMapIf6(classPool, classFile)
            }
        },
        "arc.graphics.g2d.DrawRequest" to clz@{
            modifiers = modifiers or Modifier.PUBLIC
//            addField(CtField.make("public int zOffset;", this@clz))
        }
    )

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        val output = outputs.file("${input.nameWithoutExtension}.patched.jar")
        doTransform(input, output)
    }

    private fun doTransform(input: File, output: File) {
        val pool = ClassPool()
        pool.appendSystemPath()
        pool.appendClassPath(input.path)

        val tmp = output.resolveSibling("tmp")
        if (tmp.exists()) tmp.deleteRecursively()
        tmp.mkdirs()
        transform.forEach { (clz, block) ->
            pool.get(clz).also(block)
                .writeFile(tmp.path)
        }

        val overwrite = tmp.walk().associateBy { it.toRelativeString(tmp).replace(File.separatorChar, '/') }
        ZipOutputStream(output.outputStream()).use { out ->
            ZipFile(input).use { zip ->
                for (entry in zip.entries()) {
                    out.putNextEntry(entry)
                    (overwrite[entry.name]?.also {
                        println("patchArc ${entry.name}")
                    }?.inputStream() ?: zip.getInputStream(entry)).copyTo(out)
                    out.closeEntry()
                }
            }
        }
        tmp.deleteRecursively()
    }
}

val artifactType = Attribute.of("artifactType", String::class.java)
val patched = Attribute.of("patched", Boolean::class.javaObjectType)
tasks {
    dependencies {
        attributesSchema {
            attribute(patched)
        }
        artifactTypes.getByName("jar") {
            attributes.attribute(patched, false)
        }
        configurations.named("api") {
            val arcLib = dependencies.find { it.name == "arc-core" }
                ?: error("Can't find arc-core")
            (arcLib as ExternalModuleDependency).attributes {
                attribute(patched, true)
            }
        }
        registerTransform(PatchArc::class) {
            from.attribute(artifactType, "jar").attribute(patched, false)
            to.attribute(artifactType, "jar").attribute(patched, true)
        }
    }
}