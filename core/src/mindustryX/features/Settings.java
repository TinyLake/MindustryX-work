package mindustryX.features;

import arc.*;
import arc.func.*;
import arc.scene.style.*;
import arc.struct.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.dialogs.SettingsMenuDialog.*;

import static mindustry.Vars.maxSchematicSize;

public class Settings{
    public static class LazySettingsCategory extends SettingsCategory{
        private final Prov<Drawable> iconProv;

        public LazySettingsCategory(String name, Prov<Drawable> icon, Cons<SettingsTable> builder){
            super(name, null, builder);
            iconProv = icon;
        }

        public void init(){
            icon = iconProv.get();
        }
    }

    public static final Seq<LazySettingsCategory> categories = new Seq<>();

    public static void addSettings(){
        categories.add(new LazySettingsCategory("@settings.category.mindustryX", () -> Icon.box, (c) -> {
            c.checkPref("showUpdateDialog", true);
            c.checkPref("githubMirror", false);

            c.addCategory("gameSettings");
            c.checkPref("deadOverlay", false);
            c.checkPref("invertMapClick", false);

            c.addCategory("arcReWork");
            c.checkPref("replayRecord", false);
            c.checkPref("menuFloatText", true);
            c.checkPref("researchViewer", false);
            c.sliderPref("minimapSize", 140, 40, 400, 10, i -> i + "");
            c.sliderPref("maxSchematicSize", 64, 64, 257, 1, v -> {
                maxSchematicSize = v == 257 ? Integer.MAX_VALUE : v;
                return v == 257 ? "无限" : String.valueOf(v);
            });
            {
                var v = Core.settings.getInt("maxSchematicSize");
                maxSchematicSize = v == 257 ? Integer.MAX_VALUE : v;
            }

            c.addCategory("blockSettings");
            c.checkPref("staticShieldsBorder", false);
            c.checkPref("arcTurretPlaceCheck", false);
            c.checkPref("arcchoiceuiIcon", false);
            c.sliderPref("HiddleItemTransparency", 0, 0, 100, 2, i -> i > 0 ? i + "%" : "关闭");
            c.sliderPref("overdrive_zone", 0, 0, 100, 2, i -> i > 0 ? i + "%" : "关闭");
            c.checkPref("arcPlacementEffect", false);
            c.sliderPref("blockbarminhealth", 0, 0, 4000, 50, i -> i + "[red]HP");
            c.sliderPref("blockRenderLevel", 2, 0, 2, 1, s -> switch(s){
                case 0 -> "隐藏全部建筑";
                case 1 -> "只显示建筑状态";
                default -> "全部显示";
            });

            c.addCategory("entitySettings");
            c.checkPref("bulletShow", true);
            c.checkPref("showMineBeam".toLowerCase(), true);
            c.checkPref("noPlayerHitBox", false);
            c.checkPref("payloadpreview", true);

            c.addCategory("developerMode");
            c.checkPref("renderMerge", true);
            c.checkPref("renderSort", false);
        }));
        ArcOld.init(categories);
        Events.on(ClientLoadEvent.class, e -> {
            categories.each(LazySettingsCategory::init);
            Vars.ui.settings.getCategories().addAll(categories);
        });
    }

    public static void toggle(String name){
        Core.settings.put(name, !Core.settings.getBool(name));
    }

    public static void cycle(String name, int max){
        Core.settings.put(name, (Core.settings.getInt(name) + 1) % max);
    }
}
