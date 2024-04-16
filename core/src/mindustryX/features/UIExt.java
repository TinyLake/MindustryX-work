package mindustryX.features;

import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.*;

public class UIExt{
    public static TeamSelectDialog teamSelect;

    public static void init(){
        teamSelect = new TeamSelectDialog();
    }

    public static void buildPositionRow(Table tt, Vec2 vec){
        tt.add("x= ");
        TextField x = tt.field(Strings.autoFixed(vec.x, 2), text -> {
            vec.x = Float.parseFloat(text);
        }).valid(Strings::canParseFloat).maxTextLength(8).get();

        tt.add("y= ").marginLeft(32f);
        TextField y = tt.field(Strings.autoFixed(vec.y, 2), text -> {
            vec.y = Float.parseFloat(text);
        }).valid(Strings::canParseFloat).maxTextLength(8).get();

        tt.button(UnitTypes.gamma.emoji(), () -> {
            vec.set(player.tileX(), player.tileY());
            x.setText(String.valueOf(vec.x));
            y.setText(String.valueOf(vec.y));
        }).tooltip(b -> b.label(() -> "选择玩家当前位置：" + player.tileX() + "," + player.tileY())).height(50f);

        tt.button(StatusEffects.blasted.emoji(), () -> {
            var last = MarkerType.getLastPos();
            if(last == null) return;
            vec.set(World.toTile(last.getX()), World.toTile(last.getY()));
            x.setText(String.valueOf(vec.x));
            y.setText(String.valueOf(vec.y));
        }).height(50f).tooltip((t) -> t.label(() -> {
            var last = MarkerType.getLastPos();
            if(last == null) return "[red]未标记";
            return "选择上个标记点：" + FormatDefault.formatTile(last);
        }));
    }

    public static void sendChatMessage(String message){
        Call.sendChatMessage(ui.chatfrag.mode.normalizedPrefix() + message);
    }
}
