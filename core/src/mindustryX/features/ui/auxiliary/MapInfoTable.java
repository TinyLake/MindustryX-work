package mindustryX.features.ui.auxiliary;


import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.content.*;
import mindustry.editor.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustryX.features.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.*;

public class MapInfoTable extends AuxiliaryTools.Table{
    public MapInfoTable(){
        super(Icon.map);
        defaults().size(40);

        MapInfoDialog mapInfoDialog = new MapInfoDialog();
        button(Icon.map, RStyles.clearAccentNonei, mapInfoDialog::show).tooltip("地图信息");
        button(Items.copper.emoji(), RStyles.clearLineNonet, this::floorStatisticDialog).tooltip("矿物信息");
        button(Icon.chatSmall, RStyles.clearAccentNonei, () -> UIExt.arcMessageDialog.show()).tooltip("中央监控室");
        button(Icon.playersSmall, RStyles.clearAccentNonei, () -> {
            var players = Groups.player.copy();
            if(players.isEmpty()) return;
            if(control.input instanceof DesktopInput){
                ((DesktopInput)control.input).panning = true;
            }
            InputHandler.follow = players.get((players.indexOf(InputHandler.follow, true) + 1) % players.size);
            UIExt.announce("视角追踪：" + InputHandler.follow.name);
        }).tooltip("切换跟踪玩家");
        if(!mobile) button(Icon.editSmall, RStyles.clearAccentNonei, this::uiTable).tooltip("ui大全");
    }

    private void floorStatisticDialog(){
        BaseDialog dialog = new BaseDialog("ARC-矿物统计");
        Table table = dialog.cont;
        table.clear();

        table.table(c -> {
            c.add("地表矿").color(Pal.accent).center().fillX().row();
            c.image().color(Pal.accent).fillX().row();
            c.table(list -> {
                int i = 0;
                for(Block block : content.blocks().select(b -> b instanceof Floor f && !f.wallOre && f.itemDrop != null)){
                    if(indexer.floorOresCount[block.id] == 0) continue;
                    if(i++ % 4 == 0) list.row();
                    list.add(block.emoji() + " " + block.localizedName + "\n" + indexer.floorOresCount[block.id]).width(100f).height(50f);
                }
            }).row();

            c.add("墙矿").color(Pal.accent).center().fillX().row();
            c.image().color(Pal.accent).fillX().row();
            c.table(list -> {
                int i = 0;
                for(Block block : content.blocks().select(b -> ((b instanceof Floor f && f.wallOre) || b instanceof StaticWall) && b.itemDrop != null)){
                    if(indexer.wallOresCount[block.id] == 0) continue;
                    if(i++ % 4 == 0) list.row();
                    list.add(block.emoji() + " " + block.localizedName + "\n" + indexer.wallOresCount[block.id]).width(100f).height(50f);
                }
            }).row();

            c.add("液体").color(Pal.accent).center().fillX().row();
            c.image().color(Pal.accent).fillX().row();
            c.table(list -> {
                int i = 0;
                for(Block block : content.blocks().select(b -> ((b instanceof Floor f && f.liquidDrop != null)))){
                    if(indexer.floorOresCount[block.id] == 0) continue;
                    if(i++ % 4 == 0) list.row();
                    list.add(block.emoji() + " " + block.localizedName + "\n" + indexer.floorOresCount[block.id]).width(100f).height(50f);
                }
            }).row();
        });
        dialog.addCloseButton();
        dialog.show();
    }

    private void uiTable(){
        BaseDialog dialog = new BaseDialog("ARC-ui大全");
        TextField sField = dialog.cont.field("", text -> {
        }).fillX().get();
        dialog.cont.row();

        dialog.cont.pane(c -> {
            c.add("颜色").color(Pal.accent).center().fillX().row();
            c.image().color(Pal.accent).fillX().row();
            c.table(ct -> {
                int i = 0;
                for(var colorEntry : Colors.getColors()){
                    Color value = colorEntry.value;
                    String key = colorEntry.key;
                    ct.button("[#" + value + "]" + key, Styles.cleart, () -> {
                        Core.app.setClipboardText("[#" + value + "]");
                        sField.setText(sField.getText() + "[#" + value + "]");
                    }).size(50f).tooltip(key);
                    i += 1;
                    if(i % 15 == 0) ct.row();
                }
            }).row();
            c.add("物品").color(Pal.accent).center().fillX().row();
            c.image().color(Pal.accent).fillX().row();
            c.table(ct -> {
                int i = 0;
                for(var it : Fonts.stringIcons){
                    ct.button(it.key, Styles.cleart, () -> {
                        Core.app.setClipboardText(it.value);
                        sField.setText(sField.getText() + it.value);
                    }).size(50f).tooltip(name);
                    i += 1;
                    if(i % 15 == 0) ct.row();
                }
            }).row();
            c.add("图标").color(Pal.accent).center().fillX().row();
            c.image().color(Pal.accent).fillX().row();
            c.table(ct -> {
                int i = 0;
                for(var it : Iconc.codes){
                    String icon = String.valueOf((char)it.value), internal = it.key;
                    ct.button(icon, Styles.cleart, () -> {
                        Core.app.setClipboardText(icon);
                        sField.setText(sField.getText() + icon);
                    }).size(50f).tooltip(internal);
                    i += 1;
                    if(i % 15 == 0) ct.row();
                }
            }).row();
        }).row();

        dialog.addCloseButton();
        dialog.show();
    }

}
