package mindustryX.features;

import arc.*;
import mindustry.*;
import mindustry.game.EventType.*;

public class LogicExt{
    public static boolean limitUpdate = false;
    public static int limitDst = 0, limitTimer = 10;
    public static boolean terrainSchematic = false;

    public static void init(){
        Events.run(Trigger.update, () -> {
            limitUpdate = Core.settings.getBool("limitupdate");
            limitDst = Core.settings.getInt("limitdst") * Vars.tilesize;
            if(limitUpdate && limitTimer-- < 0){
                limitUpdate = false;
                limitTimer = 10;
            }
            terrainSchematic = Core.settings.getBool("terrainSchematic");
        });
    }
}