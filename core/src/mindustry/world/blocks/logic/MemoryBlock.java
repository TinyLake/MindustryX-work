package mindustry.world.blocks.logic;

import arc.*;
import arc.graphics.Color;
import arc.scene.event.*;
import arc.scene.ui.layout.Table;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.ui.Styles;
import mindustry.world.*;
import mindustry.world.meta.*;
import mindustryX.features.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.*;

public class MemoryBlock extends Block{
    public int memoryCapacity = 32;

    public MemoryBlock(String name){
        super(name);
        destructible = true;
        solid = true;
        group = BlockGroup.logic;
        drawDisabled = false;
        envEnabled = Env.any;
        canOverdrive = false;
        configurable = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.memoryCapacity, memoryCapacity, StatUnit.none);
    }

    public boolean accessible(){
        return !privileged || state.rules.editor;
    }

    @Override
    public boolean canBreak(Tile tile){
        return accessible();
    }

    public class MemoryBuild extends Building{
        public double[] memory = new double[memoryCapacity];
        private static int numPerRow = 10;
        private static final Format format = new Format(0, true);

        //massive byte size means picking up causes sync issues
        @Override
        public boolean canPickup(){
            return false;
        }

        @Override
        public boolean collide(Bullet other){
            return !privileged;
        }

        @Override
        public boolean displayable(){
            return accessible();
        }

        @Override
        public void damage(float damage){
            if(privileged) return;
            super.damage(damage);
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.i(memory.length);
            for(double v : memory){
                write.d(v);
            }
        }

        @Override
        public void buildConfiguration(Table table){
            if(!RenderExt.showOtherInfo && !accessible()){
                //go away
                deselect();
                return;
            }

            Table vars = new Table();
            table.background(Styles.black3);
            table.table(t -> {
                t.add("每行 " + numPerRow).get();
                t.slider(2, 15, 1, numPerRow, res -> {
                    numPerRow = (int)res;
                    vars.clear();
                    buildVarsPane(vars);
                    table.pack();
                });

                t.add("保留小数: ");
                t.slider(0, 8, 1, format.getDecimal(), res -> format.setDecimal((int)res));
            }).row();
            table.pane(vars).maxWidth(1000f).maxHeight(500f).pad(4);

            buildVarsPane(vars);
        }

        public void buildVarsPane(Table t){
            for(int i = 0; i < memory.length; i++){
                int finalI = i;
                t.add("" + i).color(Color.lightGray);
                t.label(() -> format.format((float)memory[finalI])).padLeft(8)
                .touchable(Touchable.enabled).get().tapped(() -> {
                    Core.app.setClipboardText(memory[finalI] + "");
                    UIExt.announce("[cyan]复制内存[white]\n " + memory[finalI]);
                });
                if((i + 1) % numPerRow == 0) t.row();
                else t.add("|").color(((i % numPerRow) % 2 == 0) ? Color.cyan : Color.acid)
                .padLeft(12).padRight(12);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            int amount = read.i();
            for(int i = 0; i < amount; i++){
                double val = read.d();
                if(i < memory.length) memory[i] = val;
            }
        }
    }
}
