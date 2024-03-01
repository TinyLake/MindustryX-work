package mindustryX.features;

import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
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

//            tt.button(StatusEffects.blasted.emoji(), () -> {
//                if(Marker.markList.size == 0) return;
//                vec.set(World.toTile(Marker.markList.peek().markPos.x), World.toTile(Marker.markList.peek().markPos.y));
//                x.setText(World.toTile(Marker.markList.peek().markPos.x) + "");
//                y.setText(World.toTile(Marker.markList.peek().markPos.y) + "");
//            }).tooltip(Marker.markList.size == 0 ? "[red]未标记" : ("选择上个标记点：" + World.toTile(Marker.markList.peek().markPos.x) + "," + World.toTile(Marker.markList.peek().markPos.y))).height(50f);
    }
}
