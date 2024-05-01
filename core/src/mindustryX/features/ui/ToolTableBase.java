package mindustryX.features.ui;

import arc.scene.ui.layout.*;
import mindustry.ui.*;

public abstract class ToolTableBase extends Table{
    public String icon = "";
    public boolean expand = false;


    public void rebuild(){
        clear();
        table().growX().left();
        if(expand){
            buildTable();
        }
        button((expand ? "" : "[lightgray]") + icon, Styles.flatBordert, () -> {
            expand = !expand;
            rebuild();
        }).right().width(40f).minHeight(40f).fillY();
    }

    protected abstract void buildTable();
}
