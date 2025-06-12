package mindustry.annotations.impl

import arc.Core
import arc.audio.Sound
import arc.files.Fi
import arc.scene.style.TextureRegionDrawable
import arc.struct.*
import arc.util.Strings
import arc.util.io.PropertiesUtils
import arc.util.serialization.Jval
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import mindustry.annotations.Annotations.StyleDefaults
import mindustry.annotations.util.Utils.err
import mindustry.annotations.util.Utils.packageName
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element

class AssetsProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    var rootDirectory: Fi
    init {
        try {
            codeGenerator.createNewFile(Dependencies.ALL_FILES, "", "jb")
        }catch (e: Exception) {
            logger.err(e.toString())
        }
        val root = Fi(codeGenerator.generatedFile.find { it.name.contains("jb") })
        rootDirectory = root.parent().parent().parent().parent().parent().parent().parent()
        root.delete()
    }

    @Throws(Exception::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        processSounds("Sounds", "$rootDirectory/core/assets/sounds", "arc.audio.Sound", true)
        processSounds("Musics", "$rootDirectory/core/assets/music", "arc.audio.Music", false)
        processUI(Seq.with(resolver.getSymbolsWithAnnotation(StyleDefaults::class.java.canonicalName).toList()))
        return emptyList()
    }

    @OptIn(DelicateKotlinPoetApi::class)
    @Throws(Exception::class)
    fun processUI(ksAnnotateds: Seq<KSAnnotated>) {
        val type = TypeSpec.objectBuilder("Tex")
        val ictype = TypeSpec.objectBuilder("Icon")
        val ichtype = TypeSpec.objectBuilder("Iconc")
        val load = FunSpec.builder("load")
        val loadStyles = FunSpec.builder("loadStyles")
        val icload = FunSpec.builder("load")
        val ichinit = CodeBlock.builder()
        val resources = rootDirectory.toString() + "/core/assets-raw/sprites/ui"
        val icons = Jval.read(Fi.get(rootDirectory.toString() + "/core/assets-raw/fontgen/config.json").readString())

        val texIcons: ObjectMap<String, String> = OrderedMap()
        PropertiesUtils.load(texIcons, Fi.get(rootDirectory.toString() + "/core/assets/icons/icons.properties").reader())

        val iconcAll = StringBuilder()

        texIcons.each { key: String, `val`: String ->
            val split = `val`.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var name = Strings.kebabToCamel(split[1]).replace("Medium", "").replace("Icon", "").replace("Ui", "")
            if (SourceVersion.isKeyword(name) || name == "char") name += "i"
            ichtype.addProperty(
                PropertySpec.builder(name, Char::class, KModifier.FINAL).addKdoc(String.format("\\u%04x", key.toInt())).initializer(
                    "'" + (key.toInt().toChar()) + "'"
                ).build()
            )
        }

        ictype.addProperty(
            PropertySpec.builder(
                "icons",
                ObjectMap::class.parameterizedBy(String::class, TextureRegionDrawable::class),
            ).initializer("ObjectMap()").build()
        )

        ichtype.addProperty(
            PropertySpec.builder(
                "codes",
                ObjectIntMap::class.parameterizedBy(String::class),
            ).initializer("ObjectIntMap()").build()
        )

        val used = ObjectSet<String>()

        for (`val` in icons["glyphs"].asArray()) {
            val name = capitalize(`val`.getString("css", ""))

            if (!`val`.getBool("selected", true) || !used.add(name)) continue

            val code = `val`.getInt("code", 0)
            iconcAll.append(code.toChar())
            ichtype.addProperty(PropertySpec.builder(name, Char::class).addKdoc(String.format("\\u%04x", code)).initializer("'" + (code.toChar()) + "'").build())
            ichinit.addStatement("codes.put(%S, %L)", name, code)

            ictype.addProperty(PropertySpec.builder(name + "Small", TextureRegionDrawable::class.java, KModifier.LATEINIT).mutable().build())
            icload.addStatement(name + "Small = mindustry.ui.Fonts.getGlyph(mindustry.ui.Fonts.def, $code.toChar()" + ")")

            ictype.addProperty(PropertySpec.builder(name, TextureRegionDrawable::class.java, KModifier.LATEINIT).mutable().build())
            icload.addStatement("$name = mindustry.ui.Fonts.getGlyph(mindustry.ui.Fonts.icon, $code.toChar())")

            icload.addStatement("icons.put(%S, $name)", name)
            icload.addStatement("icons.put(%S, " + name + "Small)", name + "Small")
        }

        ichtype.addProperty(PropertySpec.builder("all", String::class.java).initializer("%S", iconcAll.toString()).build())
        ichtype.addInitializerBlock(ichinit.build())

        Fi.get(resources).walk { p: Fi ->
            if (!p.extEquals("png")) return@walk
            var filename = p.name()
            filename = filename.substring(0, filename.indexOf("."))

            val sfilen = filename
            val dtype = "arc.scene.style.Drawable"

            var varname = capitalize(sfilen)

            if (SourceVersion.isKeyword(varname)) varname += "s"

            type.addProperty(varname, ClassName.bestGuess(dtype))
            load.addStatement("$varname = arc.Core.atlas.drawable(%S)", sfilen)
        }

        for (elem in ksAnnotateds) {
            Seq.with((elem as KSDeclarationContainer).declarations).each({ it is KSPropertyDeclaration }, { field: Element ->
                val fname = field.simpleName.toString()
                if (fname.startsWith("default")) {
                    loadStyles.addStatement("arc.Core.scene.addStyle(" + field.asType().toString() + ".class, mindustry.ui.Styles." + fname + ")")
                }
            })
        }

        ictype.addFunction(icload.build())
        FileSpec.builder(packageName, ichtype.build().name!!).addType(ichtype.build()).build().writeTo(codeGenerator, true)
        FileSpec.builder(packageName, ictype.build().name!!).addType(ictype.build()).build().writeTo(codeGenerator, true)

        type.addFunction(load.build())
        type.addFunction(loadStyles.build())
        FileSpec.builder(packageName, type.build().name!!).addType(type.build()).build().writeTo(codeGenerator, true)
    }

    @Throws(Exception::class)
    fun processSounds(classname: String, path: String, rtype: String, genid: Boolean) {
        val type = TypeSpec.objectBuilder(classname)
        val loadBegin = FunSpec.builder("load")
        val staticb = CodeBlock.builder()

        if (genid) {
            type.addProperty(PropertySpec.builder("idToSound", IntMap::class.parameterizedBy(Sound::class), KModifier.PRIVATE).initializer("IntMap()").build())
            type.addProperty(PropertySpec.builder("soundToId", ObjectIntMap::class.parameterizedBy(Sound::class), KModifier.PRIVATE).initializer("ObjectIntMap()").build())

            type.addFunction(
                FunSpec.builder("getSoundId")
                    .addParameter("sound", Sound::class.java)
                    .returns(Int::class)
                    .addStatement("return soundToId.get(sound, -1)").build()
            )

            type.addFunction(
                FunSpec.builder("getSound")
                    .addParameter("id", Int::class)
                    .returns(Sound::class)
                    .addStatement("return idToSound.get(id) { none }").build()
            )
        }

        val names = HashSet<String>()
        val files = Seq<Fi>()
        Fi.get(path).walk { value: Fi -> files.add(value) }

        files.sortComparing { obj: Fi -> obj.name() }
        var id = 0

        for (p in files) {
            var name = p.nameWithoutExtension()

            if (names.contains(name)) {
                logger.err("Duplicate file name: $p!")
            } else {
                names.add(name)
            }

            if (SourceVersion.isKeyword(name)) name += "s"

            val filepath = path.substring(path.lastIndexOf("/") + 1) + p.path().substring(p.path().lastIndexOf(path) + path.length)

            if (genid) {
                staticb.addStatement("soundToId.put(%L, %L)", name, id)

                loadBegin.addStatement(
                    "%T.assets.load(%S, %L::class.java)\n.loaded = arc.func.Cons { it -> %L = (it as %L); soundToId.put(it, %L); idToSound.put(%L, it) }",
                    Core::class.java, filepath, rtype, name, rtype, id, id
                )
            } else {
                loadBegin.addStatement("%T.assets.load(%S, %L::class.java)\n.loaded = arc.func.Cons { %L = (it as %L) }", Core::class.java, filepath, rtype, name, rtype)
            }

            type.addProperty(PropertySpec.builder(name, ClassName.bestGuess(rtype)).mutable().initializer("$rtype()").build())

            id++
        }

        if (genid) {
            type.addInitializerBlock(staticb.build())
        }

        if (classname == "Sounds") {
            type.addProperty(PropertySpec.builder("none", ClassName.bestGuess(rtype)).initializer("$rtype()").build())
        }

        type.addFunction(loadBegin.build())

        FileSpec.builder(packageName, classname)
            .addType(type.build())
            .build()
            .writeTo(codeGenerator, true)
    }

    companion object {
        fun capitalize(s: String): String {
            val result = StringBuilder(s.length)

            for (i in 0 until s.length) {
                val c = s[i]
                if (c != '_' && c != '-') {
                    if (i > 0 && (s[i - 1] == '_' || s[i - 1] == '-')) {
                        result.append(c.uppercaseChar())
                    } else {
                        result.append(c)
                    }
                }
            }

            return result.toString()
        }
    }
}