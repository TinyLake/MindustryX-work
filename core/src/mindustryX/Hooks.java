package mindustryX;

import arc.*;
import arc.util.*;

public class Hooks implements ApplicationListener{
    /** invoke before `Vars.init`. Note that may be executed from `Vars.loadAsync` */
    public static void beforeInit(){
        Log.infoTag("MindustryX", "Hooks.beforeInit");
    }

    /** invoke after loading, just before `Mod::init` */
    @Override
    public void init(){
        Log.infoTag("MindustryX", "Hooks.init");
    }
}
