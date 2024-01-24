package mindustryX.features;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustryX.features.Settings.*;
import mindustryX.features.ui.*;

import static arc.Core.settings;
import static arc.graphics.Color.RGBtoHSV;
import static mindustry.Vars.*;

public class ArcOld{
    public static void init(Seq<LazySettingsCategory> categories){
        categories.add(new LazySettingsCategory("@settings.arc", () -> Icon.star, (c) -> {
            c.addCategory("arcHudToolbox");
            c.sliderPref("AuxiliaryTable", 0, 0, 3, 1, s -> switch(s){
                case 0 -> "关闭";
                case 1 -> "左上-右";
                case 2 -> "左上-下";
                case 3 -> "右上-下";
                default -> "";
            });
            c.checkPref("arcSpecificTable", true);
            c.checkPref("logicSupport", true);
            c.checkPref("powerStatistic", true);
            c.sliderPref("arccoreitems", 3, 0, 3, 1, s -> switch(s){
                case 0 -> "不显示";
                case 1 -> "资源状态";
                case 2 -> "兵种状态";
                default -> "显示资源和兵种";
            });
            c.sliderPref("arcCoreItemsCol", 5, 4, 15, 1, i -> i + "列");
            c.checkPref("showQuickToolTable", true);

            c.addCategory("arcCgameview");
            c.checkPref("hoveredTileInfo", false);
            c.checkPref("alwaysshowdropzone", false);
            c.checkPref("showFlyerSpawn", false);
            c.checkPref("showFlyerSpawnLine", false);
            c.checkPref("bulletShow", true);
            if(Shaders.shield != null){
                c.checkPref("staticShieldsBorder", false);
            }

            c.addCategory("arcCDisplayBlock");
            c.sliderPref("blockRenderLevel", 2, 0, 2, 1, s -> switch(s){
                case 0 -> "隐藏全部建筑";
                case 1 -> "只显示建筑状态";
                default -> "全部显示";
            });
            c.checkPref("forceEnableDarkness", true, (b) -> enableDarkness = b);
            enableDarkness = settings.getBool("forceEnableDarkness");
            c.sliderPref("HiddleItemTransparency", 0, 0, 100, 2, i -> i > 0 ? i + "%" : "关闭");
            c.sliderPref("overdrive_zone", 0, 0, 100, 2, i -> i > 0 ? i + "%" : "关闭");
            c.sliderPref("mend_zone", 0, 0, 100, 2, i -> i > 0 ? i + "%" : "关闭");
            c.checkPref("blockdisabled", false);
            c.checkPref("blockBars", false);
            c.sliderPref("blockbarminhealth", 0, 0, 4000, 50, i -> i + "[red]HP");
            c.checkPref("blockBars_mend", false);
            c.checkPref("arcdrillmode", false);
            c.checkPref("arcDrillProgress", false);
            c.checkPref("arcchoiceuiIcon", false);
            c.checkPref("arclogicbordershow", true);
            c.checkPref("arcPlacementEffect", false);

            c.checkPref("mass_driver_line", true);
            c.sliderPref("mass_driver_line_interval", 40, 8, 400, 4, i -> i / 8f + "格");
            {
                Cons<String> changed = (t) -> {
                    try{
                        RenderExt.massDriverLineColor = Color.valueOf(t);
                    }catch(Exception e){
                        RenderExt.massDriverLineColor = Color.valueOf("ff8c66");
                    }
                };
                c.textPref("mass_driver_line_color", "ff8c66", changed);
                changed.get(settings.getString("mass_driver_line_color"));
            }

            c.addCategory("arcAddTurretInfo");
            c.checkPref("showTurretAmmo", false);
            c.checkPref("showTurretAmmoAmount", false);
            c.checkPref("arcTurretPlacementItem", false);
            c.checkPref("arcTurretPlaceCheck", false);
            c.sliderPref("turretShowRange", 0, 0, 3, 1, s -> switch(s){
                case 0 -> "关闭";
                case 1 -> "仅对地";
                case 2 -> "仅对空";
                case 3 -> "全部";
                default -> "";
            });
            c.checkPref("turretForceShowRange", false);
            c.sliderPref("turretAlertRange", 0, 0, 30, 1, i -> i > 0 ? i + "格" : "关闭");
            c.checkPref("blockWeaponTargetLine", false);
            c.checkPref("blockWeaponTargetLineWhenIdle", false);

            c.addCategory("arcAddUnitInfo");
            c.checkPref("alwaysShowPlayerUnit", false);

            c.sliderPref("unitTransparency", 100, 0, 100, 5, i -> i > 0 ? i + "%" : "关闭");
            c.sliderPref("unitDrawMinHealth", settings.getInt("minhealth_unitshown", 0), 0, 2500, 50, i -> i + "[red]HP");

            c.checkPref("unitHealthBar", false);
            c.sliderPref("unitBarDrawMinHealth", settings.getInt("minhealth_unithealthbarshown", 0), 0, 2500, 100, i -> i + "[red]HP");


            c.sliderPref("unitWeaponRange", settings.getInt("unitAlertRange", 0), 0, 30, 1, s -> switch(s){
                case 0 -> "关闭";
                case 30 -> "一直开启";
                default -> s + "格";
            });
            c.sliderPref("unitWeaponRangeAlpha", settings.getInt("unitweapon_range", 0), 0, 100, 1, i -> i > 0 ? i + "%" : "关闭");

            c.checkPref("unitWeaponTargetLine", false);
            c.checkPref("showminebeam", true);
            c.checkPref("unitItemCarried", true);
            c.checkPref("unithitbox", false);
            c.checkPref("unitLogicMoveLine", false);
            c.checkPref("unitLogicTimerBars", false);
            c.checkPref("arcBuildInfo", false);
            c.checkPref("unitbuildplan", false);
            c.checkPref("arcCommandTable", true);
            c.checkPref("alwaysShowUnitRTSAi", false);
            c.sliderPref("rtsWoundUnit", 0, 0, 100, 2, s -> s + "%");

            c.addCategory("arcPlayerEffect");
            {
                Cons<String> changed = (t) -> {
                    try{
                        RenderExt.playerEffectColor = Color.valueOf(t);
                    }catch(Exception e){
                        RenderExt.playerEffectColor = Pal.accent;
                    }
                };
                c.textPref("playerEffectColor", "ffd37f", changed);
                changed.get(settings.getString("playerEffectColor"));
            }
            c.sliderPref("unitTargetType", 0, 0, 5, 1, s -> switch(s){
                case 0 -> "关闭";
                case 1 -> "虚圆";
                case 2 -> "攻击";
                case 3 -> "攻击去边框";
                case 4 -> "圆十字";
                case 5 -> "十字";
                default -> s + "";
            });
            c.sliderPref("superUnitEffect", 0, 0, 2, 1, s -> switch(s){
                case 0 -> "关闭";
                case 1 -> "独一无二";
                case 2 -> "全部玩家";
                default -> s + "";
            });
            c.sliderPref("playerEffectCurStroke", 0, 1, 30, 1, i -> (float)i / 10f + "Pixel(s)");


            c.addCategory("arcShareinfo");
            c.sliderPref("chatValidType", 0, 0, 3, 1, s -> switch(s){
                case 0 -> "原版模式";
                case 1 -> "纯净聊天";
                case 2 -> "服务器记录";
                case 3 -> "全部记录";
                default -> s + "";
            });
            c.checkPref("arcPlayerList", true);
            c.checkPref("ShowInfoPopup", true);
            c.checkPref("arcShareWaveInfo", false);
            c.checkPref("arcAlwaysTeamColor", false);
            c.checkPref("arcSelfName", false);

            c.addCategory("arcWeakCheat");
            c.checkPref("save_more_map", false);
            c.checkPref("forceIgnoreAttack", false);
            c.checkPref("allBlocksReveal", false, b -> AdvanceToolTable.allBlocksReveal = b);
            AdvanceToolTable.allBlocksReveal = settings.getBool("allBlocksReveal");
            c.checkPref("worldCreator", false, b -> AdvanceToolTable.worldCreator = b);
            AdvanceToolTable.worldCreator = settings.getBool("worldCreator");
            c.checkPref("overrideSkipWave", false);
            c.checkPref("forceConfigInventory", false);
            c.addCategory("arcStrongCheat");
            c.checkPref("showOtherTeamResource", false);
            c.checkPref("showOtherTeamState", false);
            c.checkPref("playerNeedShooting", false);
        }));
        categories.add(new LazySettingsCategory("@settings.specmode", () -> Icon.info, (c) -> {
            c.addCategory("moreContent");
            c.checkPref("override_boss_shown", false);
            c.sliderPref("minimapSize", 140, 40, 400, 10, i -> i + "");
            c.sliderPref("maxSchematicSize", 64, 64, 257, 1, v -> {
                maxSchematicSize = v == 257 ? Integer.MAX_VALUE : v;
                return v == 257 ? "无限" : String.valueOf(v);
            });
            c.sliderPref("itemSelectionHeight", 4, 4, 12, i -> i + "行");
            c.sliderPref("itemSelectionWidth", 4, 4, 12, i -> i + "列");
            c.sliderPref("blockInventoryWidth", 3, 3, 16, i -> i + "");
            c.sliderPref("editorBrush", 4, 3, 12, i -> i + "");
            c.checkPref("autoSelSchematic", false);
            c.checkPref("researchViewer", false);
            c.checkPref("arcShareMedia", true);


            c.addCategory("arcRadar");
            c.sliderPref("radarMode", 0, 0, 30, 1, s -> switch(s){
                case 0 -> "关闭";
                case 30 -> "一键开关";
                default -> "[lightgray]x[white]" + Strings.autoFixed(s * 0.2f, 1) + "倍搜索";
            });
            c.sliderPref("radarSize", 0, 0, 50, 1, s -> {
                if(s == 0) return "固定大小";
                return "[lightgray]x[white]" + Strings.autoFixed(s * 0.1f, 1) + "倍";
            });

            c.addCategory("personalized");
            c.checkPref("menuFloatText", true);
            c.sliderPref("menuFlyersCount", 0, -15, 50, 5, i -> i + "");
            c.checkPref("menuFlyersRange", false);
            c.checkPref("menuFlyersFollower", false);
            c.checkPref("colorizedContent", false);
            c.textPref("arcBackgroundPath", "");

            c.addCategory("developerMode");
            c.checkPref("rotateCanvas", false);
            c.checkPref("limitupdate", false, v -> {
                if(!v) return;
                settings.put("limitupdate", false);
                ui.showConfirm("确认开启限制更新", "此功能可以大幅提升fps，但会导致视角外的一切停止更新\n在服务器里会造成不同步\n强烈不建议在单人开启\n\n[darkgray]在帧数和体验里二选一", () -> {
                    settings.put("limitupdate", true);
                });
            });
            c.sliderPref("limitdst", 10, 0, 100, 1, s -> s + "格");
            c.checkPref("developMode", false);
        }));
    }

    public static void colorizeContent(){
        if(!settings.getBool("colorizedContent")) return;
        content.items().each(c -> colorizeContent(c, c.color));
        content.liquids().each(c -> colorizeContent(c, c.color));
        content.statusEffects().each(c -> colorizeContent(c, c.color));
        content.planets().each(c -> colorizeContent(c, c.atmosphereColor));
        content.blocks().each(c -> {
            if(c.hasColor) colorizeContent(c, blockColor(c));
            else if(c.itemDrop != null) colorizeContent(c, c.itemDrop.color);
        });
    }

    private static void colorizeContent(UnlockableContent c, Color color){
        c.localizedName = "[#" + color + "]" + c.localizedName + "[]";
    }

    private static Color blockColor(Block block){
        Color bc = new Color(0, 0, 0, 1);
        Color bestColor = new Color(0, 0, 0, 1);
        int highestS = 0;
        if(!block.synthetic()){
            PixmapRegion image = Core.atlas.getPixmap(block.fullIcon);
            for(int x = 0; x < image.width; x++)
                for(int y = 0; y < image.height; y++){
                    bc.set(image.get(x, y));
                    int s = RGBtoHSV(bc)[1] * RGBtoHSV(bc)[1] + RGBtoHSV(bc)[2] + RGBtoHSV(bc)[2];
                    if(s > highestS){
                        highestS = s;
                        bestColor = bc;
                    }
                }
        }else{
            return block.mapColor.cpy().mul(1.2f);
        }
        return bestColor;
    }
}
