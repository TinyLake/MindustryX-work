package mindustryX.features.ui.auxiliary;

import arc.*;
import arc.input.*;
import arc.scene.*;
import arc.scene.event.*;
import mindustryX.features.ui.AuxiliaryTools.*;
import mindustry.gen.*;
import mindustry.ui.fragments.*;
import mindustryX.features.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ui.RStyles.*;

public class MarkTable extends Table{
    public final Element mobileHitter = new Element();

    public MarkTable(){
        super(Icon.effect);

        mobileHitter.addListener(new ElementGestureListener(20, 0.4f, MarkerType.heatTime / 60f, 0.15f){
            @Override
            public boolean longPress(Element actor, float x, float y){
                MarkerType.selected.markWithMessage(Core.input.mouseWorld());
                mobileHitter.remove();
                return true;
            }

            @Override
            public void fling(InputEvent event, float velocityX, float velocityY, KeyCode button){
                mobileHitter.remove();
                ui.announce("[yellow]你已退出标记模式");
            }
        });

        mobileHitter.fillParent = true;
    }

    @Override
    protected void setup(){
        if(mobile){
            button("♐ >", clearLineNonet, () -> {
                ui.hudGroup.addChild(mobileHitter);
                ui.announce("[cyan]你已进入标记模式,长按屏幕可进行一次标记(外划可以退出).");
            }).height(40).width(70f).tooltip("开启手机标记");
        }

        for(var type : MarkerType.allTypes){
            button(type.shortName(), clearLineNoneTogglet, () -> MarkerType.selected = type)
            .checked(b -> MarkerType.selected == type).size(40).tooltip(type.localizedName);
        }

        button("T", clearLineNoneTogglet, () -> ui.chatfrag.nextMode())
        .checked(b -> ui.chatfrag.mode == ChatFragment.ChatMode.team).size(40).tooltip("前缀添加/t");
    }
}
