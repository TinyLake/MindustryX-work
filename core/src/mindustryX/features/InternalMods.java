package mindustryX.features;

import arc.files.*;
import arc.struct.*;
import mindustry.core.*;
import mindustry.mod.*;
import mindustry.mod.Mods.*;
import mindustryX.*;

import static arc.Core.files;
import static mindustry.Vars.*;

public class InternalMods{
    public static Seq<LoadedMod> load(){
        Seq<LoadedMod> mods = new Seq<>();
        if(!VarsX.isLoader)
            mods.add(internalMod(meta("MindustryX", "MindustryX", Version.mdtXBuild, "")));
        return mods;
    }

    private static ModMeta meta(String id, String displayName, String version, String author){
        ModMeta meta = new ModMeta();
        meta.name = id;
        meta.displayName = "[内置]" + displayName;
        meta.version = version;
        meta.author = author;
        meta.minGameVersion = Version.buildString();
        meta.hidden = true;
        meta.cleanup();
        return meta;
    }

    private static LoadedMod internalMod(ModMeta meta, Mod main){
        Fi file = modDirectory.child("internal-" + meta.name + ".jar");
        Fi root = files.internal("/mindustryX/mods/" + meta.name);
        return new LoadedMod(file, root, main, InternalMods.class.getClassLoader(), meta);
    }

    private static LoadedMod internalMod(ModMeta meta){
        return internalMod(meta, new Mod(){
        });
    }
}
