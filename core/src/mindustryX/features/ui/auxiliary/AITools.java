package mindustryX.features.ui.auxiliary;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustryX.features.ui.*;
import mindustryX.features.ui.auxiliary.ai.*;

import static mindustry.Vars.*;

public class AITools extends AuxiliaryTools.Table{
    private AIController selectAI;

    public AITools(){
        super(Icon.android);

        Events.run(EventType.Trigger.update, () -> {
            if(selectAI != null){
                selectAI.unit(player.unit());
                selectAI.updateUnit();
            }
        });

        button(Icon.settingsSmall, RStyles.clearLineNoneTogglei, 30, this::showSettingDialog);

        if(false) aiButton(new ATRIAI(), Blocks.worldProcessor.region, "ATRI AI");
        aiButton(new ArcMinerAI(), UnitTypes.mono.region, "矿机AI");
        aiButton(new ArcBuilderAI(), UnitTypes.poly.region, "重建AI");
        aiButton(new ArcRepairAI(), UnitTypes.mega.region, "修复AI");
        aiButton(new DefenderAI(), UnitTypes.oct.region, "保护AI");
    }

    private void aiButton(AIController ai, TextureRegion textureRegion, String describe){
        button(new TextureRegionDrawable(textureRegion), RStyles.clearLineNoneTogglei, 30, () -> {
            if(selectAI != null) selectAI = null;
            else selectAI = ai;
        }).checked(b -> selectAI == ai).size(40).tooltip(describe);
    }

    private void showSettingDialog(){
        int cols = (int)Math.max(Core.graphics.getWidth() / Scl.scl(480), 1);

        BaseDialog dialog = new BaseDialog("ARC-AI设定器");

        dialog.cont.table(t -> {
            t.add("minerAI-矿物筛选器").color(Pal.accent).pad(cols / 2f).center().row();

            t.image().color(Pal.accent).fillX().row();

            t.table(c -> {
                c.add("地表矿").row();

                c.table(list -> {
                    int i = 0;
                    for(Block block : ArcMinerAI.oreAllList){
                        if(indexer.floorOresCount[block.id] == 0) continue;
                        if(i++ % 3 == 0) list.row();
                        list.button(block.emoji() + "\n" + indexer.floorOresCount[block.id], Styles.flatToggleMenut, () -> {
                            if(ArcMinerAI.oreList.contains(block)) ArcMinerAI.oreList.remove(block);
                            else if(!ArcMinerAI.oreList.contains(block)) ArcMinerAI.oreList.add(block);
                        }).tooltip(block.localizedName).checked(k -> ArcMinerAI.oreList.contains(block)).width(100f).height(50f);
                    }
                }).row();

                c.add("墙矿").row();

                c.table(list -> {
                    int i = 0;
                    for(Block block : ArcMinerAI.oreAllWallList){
                        if(indexer.wallOresCount[block.id] == 0) continue;
                        if(i++ % 3 == 0) list.row();
                        list.button(block.emoji() + "\n" + indexer.wallOresCount[block.id], Styles.flatToggleMenut, () -> {
                            if(ArcMinerAI.oreWallList.contains(block)) ArcMinerAI.oreWallList.remove(block);
                            else if(!ArcMinerAI.oreWallList.contains(block)) ArcMinerAI.oreWallList.add(block);
                        }).tooltip(block.localizedName).checked(k -> ArcMinerAI.oreWallList.contains(block)).width(100f).height(50f);
                    }
                }).row();

            }).growX();
        }).growX().row();

        dialog.cont.table(t -> {
            t.add("builderAI").color(Pal.accent).pad(cols / 2f).center().row();

            t.image().color(Pal.accent).fillX().row();

            t.table(tt -> {
                tt.add("重建冷却时间: ");

                TextField sField = tt.field(ArcBuilderAI.rebuildTime + "", text -> ArcBuilderAI.rebuildTime = Math.max(5f, Float.parseFloat(text))).valid(Strings::canParsePositiveFloat).width(200f).get();

                tt.slider(5, 200, 5, i -> {
                    ArcBuilderAI.rebuildTime = i;
                    sField.setText(ArcBuilderAI.rebuildTime + "");
                }).width(200f);
            }).growX();
        }).growX();

        dialog.addCloseButton();
        dialog.show();
    }

}
