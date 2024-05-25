package mindustryX.features;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.arcModule.draw.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.BaseTurret.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.logic.MessageBlock.*;
import mindustry.world.blocks.production.Drill.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.tilesize;

public class RenderExt{
    public static boolean bulletShow, showMineBeam, displayAllMessage;
    public static boolean arcChoiceUiIcon;
    public static boolean researchViewer;
    public static boolean showPlacementEffect;
    public static int hiddenItemTransparency;
    public static int blockBarMinHealth;
    public static float overdriveZoneTransparency;
    public static boolean logicDisplayNoBorder, arcDrillMode;
    public static int blockRenderLevel;
    public static boolean renderSort;
    public static boolean drawBlockDisabled;

    public static boolean unitHide = false;

    private static Effect placementEffect;

    public static void init(){
        placementEffect = new Effect(0f, e -> {
            Draw.color(e.color);
            float range = e.rotation;
            Lines.stroke((1.5f - e.fin()) * (range / 100));
            if(e.fin() < 0.7f) Lines.circle(e.x, e.y, (float)((1 - Math.pow((0.7f - e.fin()) / 0.7f, 2f)) * range));
            else{
                Draw.alpha((1 - e.fin()) * 5f);
                Lines.circle(e.x, e.y, range);
            }
        });

        Events.run(Trigger.preDraw, () -> {
            bulletShow = Core.settings.getBool("bulletShow");
            showMineBeam = !unitHide && Core.settings.getBool("showminebeam");
            displayAllMessage = Core.settings.getBool("displayallmessage");
            arcChoiceUiIcon = Core.settings.getBool("arcchoiceuiIcon");
            researchViewer = Core.settings.getBool("researchViewer");
            showPlacementEffect = Core.settings.getBool("arcPlacementEffect");
            hiddenItemTransparency = Core.settings.getInt("HiddleItemTransparency");
            blockBarMinHealth = Core.settings.getInt("blockbarminhealth");
            overdriveZoneTransparency = Core.settings.getInt("overdrive_zone") / 100f;
            logicDisplayNoBorder = Core.settings.getBool("arclogicbordershow");
            arcDrillMode = Core.settings.getBool("arcdrillmode");
            blockRenderLevel = Core.settings.getInt("blockRenderLevel");
            renderSort = Core.settings.getBool("renderSort");
            drawBlockDisabled = Core.settings.getBool("blockdisabled");
        });
        Events.run(Trigger.draw, RenderExt::draw);
        Events.on(TileChangeEvent.class, RenderExt::onSetBlock);
    }

    private static void draw(){

    }

    public static void onGroupDraw(Drawc t){
        if(!bulletShow && t instanceof Bulletc) return;
        t.draw();
    }

    public static void onBlockDraw(Tile tile, Block block, Building build){
        if(blockRenderLevel < 2) return;
        block.drawBase(tile);
        if(build instanceof BaseTurretBuild turretBuild)
            ARCBuilds.arcTurret(turretBuild);
        if(arcDrillMode && build instanceof DrillBuild drill)
            arcDrillModeDraw(block, drill);
        if(displayAllMessage && build instanceof MessageBuild)
            Draw.draw(Layer.overlayUI - 0.1f, build::drawSelect);
    }

    private static void placementEffect(float x, float y, float lifetime, float range, Color color){
        placementEffect.lifetime = lifetime;
        placementEffect.at(x, y, range, color);
    }

    public static void onSetBlock(TileChangeEvent event){
        Building build = event.tile.build;
        if(build != null && showPlacementEffect){
            if(build.block instanceof BaseTurret t && build.health > blockBarMinHealth)
                placementEffect(build.x, build.y, 120f, t.range, build.team.color);
            else if(build.block instanceof Radar t)
                placementEffect(build.x, build.y, 120f, t.fogRadius * tilesize, build.team.color);
            else if(build.block instanceof CoreBlock t)
                placementEffect(build.x, build.y, 180f, t.fogRadius * tilesize, build.team.color);
            else if(build.block instanceof MendProjector t)
                placementEffect(build.x, build.y, 120f, t.range, Pal.heal);
            else if(build.block instanceof OverdriveProjector t)
                placementEffect(build.x, build.y, 120f, t.range, t.baseColor);
            else if(build.block instanceof LogicBlock t)
                placementEffect(build.x, build.y, 120f, t.range, t.mapColor);
        }
    }

    /** 在转头旁边显示矿物类型 */
    private static void arcDrillModeDraw(Block block, DrillBuild build){
        Item dominantItem = build.dominantItem;
        if(dominantItem == null) return;
        int size = block.size;
        float dx = build.x - size * tilesize / 2f + 5, dy = build.y - size * tilesize / 2f + 5;
        float iconSize = 5f;
        Draw.rect(dominantItem.fullIcon, dx, dy, iconSize, iconSize);
        Draw.reset();

        float eff = Mathf.lerp(0, 1, Math.min(1f, (float)build.dominantItems / (size * size)));
        if(eff < 0.9f){
            Draw.alpha(0.5f);
            Draw.color(dominantItem.color);
            Lines.stroke(1f);
            Lines.arc(dx, dy, iconSize * 0.75f, eff);
        }
    }
}
