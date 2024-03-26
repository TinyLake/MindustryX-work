package mindustryX.features;

import arc.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.world.*;

public class RenderExt{
    public static void init(){
        Events.run(Trigger.preDraw, () -> {
        });
        Events.run(Trigger.draw, RenderExt::draw);
    }

    private static void draw(){

    }

    public static void onGroupDraw(Drawc t){
        t.draw();
    }

    public static void onBlockDraw(Tile tile, Block block, @Nullable Building build){
        block.drawBase(tile);
    }
}
