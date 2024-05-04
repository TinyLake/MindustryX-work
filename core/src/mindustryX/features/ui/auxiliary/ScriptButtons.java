package mindustryX.features.ui.auxiliary;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.ui.dialogs.*;
import mindustryX.features.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.*;

public class ScriptButtons extends AuxiliaryTools.Table{

    public ScriptButtons(){
        super(UnitTypes.gamma.uiIcon);
    }

    @Override
    protected void setup(){
        defaults().size(40);

        scriptButton(Blocks.buildTower.uiIcon, "在建造列表加入被摧毁建筑", () -> player.buildDestroyedBlocks());

        scriptButton(Blocks.message.uiIcon, "锁定上个标记点", MarkerType::lockOnLastMark);

        scriptButton(Items.copper.uiIcon, "一键放置", () -> player.dropItems());

        scriptButton(Icon.pencilSmall, "特效显示", () -> EffectsDialog.withAllEffects().show());

        addSettingButton(Icon.modeAttack, "autotarget", "自动攻击", s -> {
        });

        addSettingButton(UnitTypes.vela.uiIcon, "forceBoost", "强制助推", s -> {
        });

        if(!mobile){
            addSettingButton(Icon.eyeSmall, "removePan", "视角脱离玩家", s -> {
                if(control.input instanceof DesktopInput desktopInput){
                    desktopInput.panning = true;
                }
            });
        }
    }

    protected void addSettingButton(TextureRegion region, String settingName, String description, Boolc onClick){
        addSettingButton(new TextureRegionDrawable(region), settingName, description, onClick);
    }

    protected void addSettingButton(Drawable icon, String settingName, String description, Boolc onClick){
        scriptButton(icon, description, () -> {
            boolean setting = Core.settings.getBool(settingName);

            Core.settings.put(settingName, !setting);
            UIExt.announce("已" + (setting ? "取消" : "开启") + description);

            onClick.get(!setting);
        }).checked(b -> Core.settings.getBool(settingName));
    }

    protected Cell<ImageButton> scriptButton(TextureRegion region, String description, Runnable runnable){
        return scriptButton(new TextureRegionDrawable(region), description, runnable);
    }

    protected Cell<ImageButton> scriptButton(Drawable icon, String description, Runnable runnable){
        return button(icon, RStyles.clearLineNonei, 30, runnable).tooltip(description);//TODO allowMobile
    }

}
