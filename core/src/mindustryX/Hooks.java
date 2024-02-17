package mindustryX;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustryX.features.*;

import java.net.*;

public class Hooks implements ApplicationListener{
    @Override
    public void init(){
        Log.infoTag("MindustryX", "Hooks.init");
        RenderExt.init();
        ArcOld.addSettings();
        if(!Vars.headless){
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
}
