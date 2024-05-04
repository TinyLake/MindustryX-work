package mindustryX.features.ui;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.game.EventType.*;
import mindustry.ui.*;
import mindustryX.features.ui.auxiliary.*;

public class AuxiliaryTools extends Table{
    private boolean shown = true;
    private final Seq<Table> toolsTables = Seq.with(
    new MapInfoTable(),
    new WaveInfoTable(),
    new AITools(),
    new ScriptButtons(),
    new MobileScriptButtons(),
    new MarkTable()
    );

    static{
        RStyles.load();
    }

    public AuxiliaryTools(){
        setup();

        rebuild();
        Events.on(WorldLoadEvent.class, e -> rebuild());
        Events.on(ResetEvent.class, e -> clearChildren());
    }

    public void setup(){
        for(Table table : toolsTables){
            table.setup();
        }
    }

    public void toggle(){
        shown = !shown;
        rebuild();
    }

    private void rebuild(){
        clearChildren();

        table(Styles.black3, buttons -> {
            buttons.button("[acid]辅助器", RStyles.clearLineNoneTogglet, this::toggle).size(80f, 40f).tooltip((shown ? "关闭" : "开启") + "辅助器");

            if(shown){
                for(Table table : toolsTables){
                    table.addButton(buttons);
                }
            }
        }).fillX();

        row();

        if(shown){
            table(RStyles.black1, body -> {
                body.defaults().expandX().left();
                for(Table table : toolsTables){
                    table.margin(4);
                    body.collapser(table, table::shown).row();
                }
            }).fillX().left();
        }
    }

    public abstract static class Table extends arc.scene.ui.layout.Table{
        private boolean shown;
        protected Drawable icon;

        public Table(TextureRegion region){
            this(new TextureRegionDrawable(region));
        }

        public Table(Drawable icon){
            this.icon = icon;
        }

        public void addButton(arc.scene.ui.layout.Table buttons){
            buttons.button(icon, RStyles.clearAccentNoneTogglei, 30, this::toggle)
            .size(40).checked(b -> shown);
        }

        protected abstract void setup();

        public boolean shown(){
            return shown;
        }

        public boolean toggle(){
            return shown = !shown;
        }
    }
}
