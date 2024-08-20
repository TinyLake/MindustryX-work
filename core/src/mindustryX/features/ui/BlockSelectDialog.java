package mindustryX.features.ui;

import arc.func.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

//move from mindustry.arcModule.ui.dialogs.BlockSelectDialog
public class BlockSelectDialog extends BaseDialog{
    private final Boolf<Block> condition;
    private final Cons<Block> cons;
    private final Boolf<Block> checked;
    private final boolean autoHide;
    private String searchBlock = "";
    private final Table blockTable = new Table();

    public BlockSelectDialog(Boolf<Block> condition, Cons<Block> cons, Boolf<Block> checked){
        this(condition, cons, checked, true);
    }

    public BlockSelectDialog(Boolf<Block> condition, Cons<Block> cons, Boolf<Block> checked, boolean autoHide){
        super("方块选择器");
        this.condition = condition;
        this.cons = cons;
        this.checked = checked;
        this.autoHide = autoHide;
        cont.pane(td -> {
            td.field("", t -> {
                searchBlock = !t.isEmpty() ? t.toLowerCase() : "";
                rebuild();
            }).maxTextLength(50).growX().get().setMessageText("搜索...");
            td.row();
            td.add(blockTable);
        });
        rebuild();
        addCloseButton();
    }

    private void rebuild(){
        blockTable.clear();
        blockTable.table(td -> {
            Seq<Block> blocks = content.blocks().select(block -> condition.get(block) && (searchBlock.isEmpty() || block.name.contains(searchBlock) || block.localizedName.contains(searchBlock)) && block.isVisible()).sort(block -> block.group.ordinal());
            Seq<BlockGroup> blockGroups = blocks.map(block -> block.group).distinct();
            blockGroups.each(blockGroup -> {
                td.row();
                td.add(blockGroup.toString()).row();
                td.image().color(Pal.accent).fillX().row();
                td.table(ttd -> blocks.select(block1 -> block1.group == blockGroup).each(block1 -> {
                    ttd.button(new TextureRegionDrawable(block1.uiIcon), Styles.cleari, iconSmall, () -> {
                        cons.get(block1);
                        if(autoHide) hide();
                    }).tooltip(block1.localizedName).pad(3f).checked(b -> checked.get(block1)).size(50f);
                    if(ttd.getChildren().size % 10 == 0) ttd.row();
                }));
            });
        });
    }
}
