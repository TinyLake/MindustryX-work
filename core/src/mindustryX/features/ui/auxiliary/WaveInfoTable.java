package mindustryX.features.ui.auxiliary;

import arc.*;
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

import static mindustry.Vars.*;

public class WaveInfoTable extends AuxiliaryTools.Table{
    public static final float fontScl = 0.8f;
    private int waveOffset = 0;
    private final Table waveInfo;

    public WaveInfoTable(){
        super(Icon.waves);

        Events.on(WorldLoadEvent.class, e -> {
            waveOffset = 0;
            rebuildWaveInfo();
        });

        Events.on(WaveEvent.class, e -> rebuildWaveInfo());

        left().top();

        ArcWaveInfoDialog waveInfoDialog = new ArcWaveInfoDialog();
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
            setWave.label(() -> "" + getDisplayWaves()).fontScale(fontScl).row();
            setWave.button(Icon.settingsSmall, RStyles.clearAccentNonei, iconMed, () -> {
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
        waveInfo = new Table(Tex.pane).left().top();
        add(new ScrollPane(waveInfo, Styles.noBarPane){
            {
                setScrollingDisabledY(true);
                setForceScroll(true, false);
                // 自动失焦
                update(() -> {
                    if(hasScroll() && !hasMouse()){
                        Core.scene.setScrollFocus(null);
                    }
                });
            }

            @Override
            public float getPrefWidth(){
                return 0f;
            }
        }).growX();
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
                groupT.image(group.type.uiIcon).scaling(Scaling.fit).size(iconSmall).row();
                groupT.add("" + amount, fontScl).row();
                groupT.add((shield > 0 ? UI.formatAmount((long)shield) : ""), fontScl).row();

                if(effect != null && effect != StatusEffects.none){
                    groupT.image(effect.uiIcon).size(iconSmall);
                }
            }).pad(4).left().top();
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
