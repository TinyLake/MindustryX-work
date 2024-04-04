package mindustryX;

import arc.*;
import arc.files.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustryX.features.*;
import mindustryX.features.Settings;

import java.net.*;
import java.util.*;

public class Hooks implements ApplicationListener{
    /** invoke before `Vars.init`. Note that may be executed from `Vars.loadAsync` */
    public static void beforeInit(){
        Log.infoTag("MindustryX", "Hooks.beforeInit");
        registerBundle();
        Settings.addSettings();
    }

    /** invoke after loading, just before `Mod::init` */
    @Override
    public void init(){
        Log.infoTag("MindustryX", "Hooks.init");
        if(AutoUpdate.INSTANCE.getActive())
            AutoUpdate.INSTANCE.checkUpdate();
        if(!Vars.headless){
            RenderExt.init();
            TimeControl.init();
            UIExt.init();
            ReplayController.init();
        }
    }

    @SuppressWarnings("unused")//call before arc.util.Http$HttpRequest.block
    public static void onHttp(Http.HttpRequest req){
        if(Core.settings.getBool("githubMirror")){
            try{
                String url = req.url;
                String host = new URL(url).getHost();
                if(host.contains("github.com") || host.contains("raw.githubusercontent.com")){
                    url = "https://gh.tinylake.tech/" + url;
                    req.url = url;
                }
            }catch(Exception e){
                //ignore
            }
        }
    }

    public static @Nullable String onHandleSendMessage(String message, @Nullable Player sender){
        if(message == null) return null;
        return message;
    }

    @Override
    public void update(){
        pollKeys();
    }

    public static void pollKeys(){
        if(Vars.headless || Core.scene.hasField()) return;
        if(Core.input.keyTap(Binding.toggle_unit)){
            RenderExt.unitHide = !RenderExt.unitHide;
        }
    }

    private static void registerBundle(){
        //MDTX: bundle overwrite
        try{
            I18NBundle originBundle = Core.bundle;
            Fi handle = Core.files.internal("bundles/bundle-mdtx");
            Core.bundle = I18NBundle.createBundle(handle, Locale.getDefault());
            Reflect.set(Core.bundle, "locale", originBundle.getLocale());
            Log.info("MDTX: bundle has been loaded.");
            var rootBundle = Core.bundle;
            while(rootBundle.getParent() != null){
                rootBundle = rootBundle.getParent();
            }
            Reflect.set(rootBundle, "parent", originBundle);
        }catch(Throwable e){
            Log.err(e);
        }
    }
}
