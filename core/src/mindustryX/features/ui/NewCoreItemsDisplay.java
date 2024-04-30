package mindustryX.features.ui;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.storage.*;

import java.util.*;

import static mindustry.Vars.*;

//moved from mindustry.arcModule.ui.RCoreItemsDisplay
public class NewCoreItemsDisplay extends Table{
    public static final float MIN_WIDTH = 64f;

    private Table itemsTable, unitsTable, plansTable;

    private static final Interval timer = new Interval(2);
    private int columns = -1;

    private final int[] itemDelta;
    private final int[] lastItemAmount;
    private final ObjectSet<Item> usedItems = new ObjectSet<>();
    private final ObjectSet<UnitType> usedUnits = new ObjectSet<>();

    private final ItemSeq planItems = new ItemSeq();
    private final ObjectIntMap<Block> planCounter = new ObjectIntMap<>();

    public NewCoreItemsDisplay(){
        itemDelta = new int[content.items().size];
        lastItemAmount = new int[content.items().size];
        Events.on(ResetEvent.class, e -> {
            usedItems.clear();
            usedUnits.clear();
            Arrays.fill(itemDelta, 0);
            Arrays.fill(lastItemAmount, 0);
        });

        setup();
    }

    private void setup(){
        itemsTable = new Table(Styles.black3);
        unitsTable = new Table(Styles.black3);
        plansTable = new Table(Styles.black3);
        plansTable.marginTop(12f);

        var itemCol = add(new SimpleCollapser(itemsTable, true)).growX().get();
        var unitsCol = row().add(new SimpleCollapser(unitsTable, true)).growX().get();
        var plansCol = row().add(new SimpleCollapser(plansTable, true)).growX().get();
        update(() -> {
            var columns = Core.settings.getInt("arcCoreItemsCol");
            int displayType = Core.settings.getInt("arccoreitems");
            itemCol.setCollapsed(!itemCol.hasChildren() || displayType != 1 && displayType != 3);
            unitsCol.setCollapsed(!unitsCol.hasChildren() || displayType != 2 && displayType != 3);
            plansCol.setCollapsed(!plansCol.hasChildren() || displayType < 1);

            if(this.columns != columns){
                this.columns = columns;
                rebuildItems();
                rebuildUnits();
                rebuildPlans();
            }
        });

        itemsTable.update(() -> {
            updateItemMeans();
            if(content.items().contains(item -> player.team().items().get(item) > 0 && usedItems.add(item))){
                rebuildItems();
            }
        });
        unitsTable.update(() -> {
            if(content.units().contains(unit -> player.team().data().countType(unit) > 0 && usedUnits.add(unit))){
                rebuildUnits();
            }
        });
        plansTable.update(() -> {
            if(timer.get(1, 10f)){
                rebuildPlans();
            }
        });
    }

    private void updateItemMeans(){
        if(!timer.get(0, 60f)) return;
        var items = player.team().items();
        for(Item item : usedItems){
            short id = item.id;
            int coreAmount = items.get(id);
            int lastAmount = lastItemAmount[id];
            itemDelta[id] = coreAmount - lastAmount;
            lastItemAmount[id] = coreAmount;
        }
    }

    private void rebuildItems(){
        itemsTable.clearChildren();
        if(player.team().core() == null) return;

        int i = 0;
        for(Item item : content.items()){
            if(!usedItems.contains(item)){
                continue;
            }

            itemsTable.stack(
            new Table(t ->
            t.image(item.uiIcon).size(iconMed).scaling(Scaling.fit).padRight(4f)
            .tooltip(tooltip -> tooltip.background(Styles.black6).margin(4f).add(item.localizedName).style(Styles.outlineLabel))
            ),
            new Table(t -> t.label(() -> {
                int update = itemDelta[item.id];
                if(update == 0) return "";
                return (update < 0 ? "[red]" : "[green]+") + UI.formatAmount(update);
            }).fontScale(0.85f)).top().left()
            ).pad(4f);

            itemsTable.table(amountTable -> {
                amountTable.defaults().expand().left();

                Label amountLabel = amountTable.add("").get();
                amountTable.row();
                Label planLabel = amountTable.add("").fontScale(0.85f).get();

                amountTable.update(() -> {
                    int planAmount = planItems.get(item);
                    int amount = player.team().items().get(item);

                    Color amountColor = Color.white;
                    if(planAmount == 0){
                        var core = player.team().core();
                        if(core != null && amount >= core.storageCapacity * 0.99){
                            amountColor = Pal.accent;
                        }

                        planLabel.setText("");
                    }else{
                        amountColor = (amount > planAmount ? Color.green
                        : amount > planAmount / 2 ? Pal.stat
                        : Color.scarlet);

                        planLabel.setColor(planAmount > 0 ? Color.scarlet : Color.green);
                        planLabel.setText(UI.formatAmount(planAmount));
                    }

                    amountLabel.setColor(amountColor);
                    amountLabel.setText(UI.formatAmount(amount));
                });
            }).padRight(4f).minWidth(MIN_WIDTH).left();

            if(++i % columns == 0){
                itemsTable.row();
            }
        }
    }

    private void rebuildUnits(){
        unitsTable.clearChildren();

        int i = 0;
        for(UnitType unit : content.units()){
            if(usedUnits.contains(unit)){
                unitsTable.image(unit.uiIcon).size(iconMed).scaling(Scaling.fit).padRight(4f)
                .tooltip(t -> t.background(Styles.black6).margin(4f).add(unit.localizedName).style(Styles.outlineLabel));
                unitsTable.label(() -> {
                    int typeCount = player.team().data().countType(unit);
                    return (typeCount == Units.getCap(player.team()) ? "[stat]" : "") + typeCount;
                }).padRight(4f).minWidth(MIN_WIDTH).left();

                if(++i % columns == 0){
                    unitsTable.row();
                }
            }
        }
    }

    private void rebuildPlans(){
        planItems.clear();
        planCounter.clear();

        control.input.allPlans().each(plan -> {
            Block block = plan.block;

            if(block instanceof CoreBlock) return;

            planCounter.increment(block, plan.breaking ? -1 : 1);

            for(ItemStack stack : block.requirements){
                int planAmount = (int)(plan.breaking ? -state.rules.buildCostMultiplier * state.rules.deconstructRefundMultiplier * stack.amount * plan.progress
                : state.rules.buildCostMultiplier * stack.amount * (1 - plan.progress));
                planItems.add(stack.item, planAmount);
            }
        });

        plansTable.clearChildren();
        if(planCounter.isEmpty()) return;
        int i = 0;
        for(Block block : content.blocks()){
            int count = planCounter.get(block, 0);
            if(count == 0 || block.category == Category.distribution && block.size < 3
            || block.category == Category.liquid && block.size < 3
            || block instanceof PowerNode
            || block instanceof BeamNode) continue;

            plansTable.image(block.uiIcon).size(iconMed).scaling(Scaling.fit).padRight(4f);
            plansTable.label(() -> (count > 0 ? "[green]+" : "[red]") + count).padRight(3).minWidth(MIN_WIDTH).left();

            if(++i % columns == 0){
                plansTable.row();
            }
        }
    }

    public boolean hadItem(Item item){
        return usedItems.contains(item);
    }
}
