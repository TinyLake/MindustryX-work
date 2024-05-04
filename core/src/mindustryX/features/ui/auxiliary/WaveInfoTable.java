package mindustryX.features.ui.auxiliary;

import arc.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustryX.features.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.state;

public class WaveInfoTable extends AuxiliaryTools.Table{
    public static final float fontScl = 0.8f;

    private int waveOffset = 0;

    private final Table waveInfo;

    private final ArcWaveInfoDialog waveInfoDialog = new ArcWaveInfoDialog();

    public WaveInfoTable(){
        super(Icon.waves);

        Events.on(WorldLoadEvent.class, e -> {
            waveOffset = 0;
            rebuildWaveInfo();
        });

        Events.on(WaveEvent.class, e -> rebuildWaveInfo());

        waveInfo = new Table(Tex.pane);
    }

    @Override
    protected void setup(){
        left().top();
        waveInfo.left().top();

        button(Icon.waves, RStyles.clearAccentNonei, waveInfoDialog::show).size(40).tooltip("波次信息");

        table(buttons -> {
            buttons.defaults().size(40);

            buttons.button("<", RStyles.clearLineNonet, () -> shiftWaveOffset(-1));

            buttons.button("O", RStyles.clearLineNonet, () -> setWaveOffset(0));

            buttons.button(">", RStyles.clearLineNonet, () -> shiftWaveOffset(1));

            buttons.button("Go", RStyles.clearLineNonet, () -> {
                state.wave += waveOffset;
                setWaveOffset(0);
            });

            buttons.button("♐", RStyles.clearLineNonet, () -> ArcMessageDialog.shareWaveInfo(state.wave + waveOffset))
            .disabled((b) -> !state.rules.waves && !Core.settings.getBool("arcShareWaveInfo"));

        }).left().row();

        table(setWave -> {
            setWave.label(() -> "" + getDisplayWaves()).get().setFontScale(fontScl);

            setWave.row();

            setWave.button(Icon.settingsSmall, RStyles.clearAccentNonei, 30, () -> {
                Dialog lsSet = new BaseDialog("波次设定");
                lsSet.cont.add("设定查询波次").padRight(5f).left();
                TextField field = lsSet.cont.field(state.wave + waveOffset + "", text -> waveOffset = Integer.parseInt(text) - state.wave).size(320f, 54f).valid(Strings::canParsePositiveInt).maxTextLength(100).get();
                lsSet.cont.row();
                lsSet.cont.slider(1, ArcWaveSpawner.calWinWave(), 1, res -> {
                    waveOffset = (int)res - state.wave;
                    field.setText((int)res + "");
                });
                lsSet.addCloseButton();
                lsSet.show();
            });
        });

        pane(Styles.noBarPane, waveInfo).scrollY(false).pad(8f).maxWidth(180f).left().update(pane -> {
            // 自动失焦
            if(pane.hasScroll()){
                Element result = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                if(result == null || !result.isDescendantOf(pane)){
                    Core.scene.setScrollFocus(null);
                }
            }
        });
    }

    private void rebuildWaveInfo(){
        waveInfo.clearChildren();

        int curInfoWave = getDisplayWaves();
        for(SpawnGroup group : state.rules.spawns){
            int amount = group.getSpawned(curInfoWave);

            if(amount == 0) continue;

            float shield = group.getShield(curInfoWave);
            StatusEffect effect = group.effect;

            waveInfo.table(groupT -> {
                groupT.image(group.type.uiIcon).scaling(Scaling.fit).size(20).row();

                groupT.add("" + amount, fontScl).row();

                groupT.add((shield > 0 ? UI.formatAmount((long)shield) : ""), fontScl).row();

                if(effect != null && effect != StatusEffects.none){
                    groupT.image(effect.uiIcon).size(20);
                }
            }).pad(8).left().top();
        }
    }

    private void shiftWaveOffset(int shiftCount){
        int offset = Math.max(waveOffset + shiftCount, -state.wave + 1);
        setWaveOffset(offset);
    }

    private void setWaveOffset(int waveOffset){
        this.waveOffset = waveOffset;
        rebuildWaveInfo();
    }

    private int getDisplayWaves(){
        return state.wave - 1 + waveOffset;
    }

}
