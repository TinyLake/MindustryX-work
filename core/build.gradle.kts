import javassist.ClassPool
import javassist.CtClass
import javassist.bytecode.Bytecode
import javassist.bytecode.Descriptor
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("org.javassist:javassist:3.30.2-GA")
    }
}
plugins {
    java
//    kotlin()
}

project.tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

sourceSets {
    main {
        java.srcDirs("src/", layout.buildDirectory.dir("/generated/sources/annotationProcessor/java/main"))
    }
}

task<Jar>("patchArc") {
    group = "build"
    destinationDirectory.set(temporaryDir)
    archiveBaseName = "patched"

    val patchSrc by configurations.creating
    dependencies {
        configurations.api.configure {
            val arcLib = dependencies.find { it.name == "arc-core" }
                    ?: error("Can't find arc-core")
            dependencies.remove(arcLib)
            patchSrc(arcLib)
        }
    }
    inputs.files(patchSrc)
    dependencies.api(files(this))
    tasks.named("processResources") {
        dependsOn(this@task)
    }

    val transform = mutableMapOf<String, CtClass.() -> Unit>()
    transform["arc.util.Http\$HttpRequest"] = clz@{
        getDeclaredMethod("block").apply {
            val code = Bytecode(methodInfo.constPool)
            val desc = Descriptor.ofMethod(CtClass.voidType, arrayOf(this@clz))
            code.addAload(0)
            code.addInvokestatic("mindustryX.Hooks", "onHttp", desc)
            methodInfo.codeAttribute.iterator().insertEx(code.get())
            methodInfo.rebuildStackMapIf6(classPool, classFile)
        }
    }

    val genDir = layout.buildDirectory.dir("generated/patched")
    from(genDir)
    from(zipTree(patchSrc.files.single())){
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    doFirst {
        genDir.get().asFileTree.forEach { it.delete() }

        val pool = ClassPool()
        pool.appendSystemPath()
        patchSrc.files.forEach {
            pool.appendClassPath(it.path)
        }

        transform.forEach { (clz, block) ->
            pool.get(clz).also(block)
                    .writeFile(genDir.get().asFile.path)
        }
    }
}