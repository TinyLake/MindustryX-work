package mindustryX;

import arc.*;
import arc.files.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustryX.features.*;
import mindustryX.features.Settings;
import mindustryX.features.ui.*;

import java.net.*;
import java.util.*;

public class Hooks implements ApplicationListener{
    /** invoke before `Vars.init`. Note that may be executed from `Vars.loadAsync` */
    public static void beforeInit(){
        Log.infoTag("MindustryX", "Hooks.beforeInit");
        registerBundle();
        Settings.addSettings();
        DebugUtil.init();//this is safe, and better at beforeInit,
    }

    /** invoke after loading, just before `Mod::init` */
    @Override
    public void init(){
        Log.infoTag("MindustryX", "Hooks.init");
        LogicExt.init();
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
        if(Vars.ui != null){
            if(MarkerType.resolveMessage(message)) return message;
            try{
                ArcMessageDialog.resolveMsg(message, sender);
                if(sender != null){
                    message = (sender.dead() ? Iconc.alphaaaa : sender.unit().type.emoji()) + " " + message;
                }
            }catch(Exception e){
                Log.err(e);
            }
        }
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
        if(Core.input.keyTap(Binding.lockonLastMark)){
            MarkerType.lockOnLastMark();
        }
        if(Core.input.keyTap(Binding.point)){
            MarkerType.selected.markWithMessage(Core.input.mouseWorld());
        }
        if(Core.input.keyTap(Binding.toggle_block_render)){
            Core.settings.put("blockRenderLevel", (RenderExt.blockRenderLevel + 1) % 3);
        }
        if(Core.input.keyTap(Binding.arcScanMode)){
            ArcScanMode.enabled = !ArcScanMode.enabled;
        }
        if(Core.input.keyTap(Binding.showRTSAi)){
            Core.settings.put("alwaysShowUnitRTSAi", !Core.settings.getBool("alwaysShowUnitRTSAi"));
        }
        if(Core.input.keyTap(Binding.superUnitEffect)){
            int level = Core.settings.getInt("superUnitEffect");
            Core.settings.put("superUnitEffect", (level + 1) % 3);
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
