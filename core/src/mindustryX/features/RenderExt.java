package mindustryX.features;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.logic.MessageBlock.*;

public class RenderExt{
    public static boolean bulletShow, showMineBeam, displayAllMessage;
    public static boolean arcChoiceUiIcon;

    public static void init(){
        Events.run(Trigger.preDraw, () -> {
            bulletShow = Core.settings.getBool("bulletShow");
            showMineBeam = Core.settings.getBool("showminebeam");
            displayAllMessage = Core.settings.getBool("displayallmessage");
            arcChoiceUiIcon = Core.settings.getBool("arcchoiceuiIcon");
        });
        Events.run(Trigger.draw, RenderExt::draw);
    }

    private static void draw(){

    }

    public static void onGroupDraw(Drawc t){
        if(!bulletShow && t instanceof Bulletc) return;
        t.draw();
        if(displayAllMessage && t instanceof MessageBuild build){
            Draw.draw(Layer.overlayUI - 0.1f, build::drawSelect);
        }
    }
}
