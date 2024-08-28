package mindustryX.features;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;
import mindustryX.features.ui.*;

import static mindustry.Vars.*;

//move from mindustry.arcModule.toolpack.ArcScanMode
public class ArcScanMode{
    public static boolean enabled = false;
    private static final Label ctTable = new Label("");
    private static final Table spawnerTable = new Table();
    private static final Table flyerTable = new Table();

    /**
     * conveyor
     */
    static final int maxLoop = 200;


    static{
        ctTable.touchable = spawnerTable.touchable = flyerTable.touchable = Touchable.disabled;
        ui.hudGroup.addChild(ctTable);
        ui.hudGroup.addChild(spawnerTable);
        ui.hudGroup.addChild(flyerTable);

        ctTable.setAlignment(Align.bottom, Align.center);
        ctTable.visible(() -> enabled && state.isPlaying());
        ctTable.update(() -> {
            ctTable.setPosition(Core.input.mouseX(), Core.input.mouseY());
            var pos = Core.input.mouseWorld();
            ctTable.setText(Strings.format("@,@\n距离: @",
            (int)(pos.x / tilesize), (int)(pos.y / tilesize), (int)(player.dst(pos) / tilesize)));
        });
    }

    public static void draw(){
        if(!enabled || !state.isPlaying()){
            ctTable.clear();
            spawnerTable.clear();
            flyerTable.clear();
            return;
        }
        updateSpawnerDisplay();
        drawTransportFlow();
    }

    private static void updateSpawnerDisplay(){
        spawnerTable.clear();
        flyerTable.clear();
        if(ArcWaveSpawner.arcWave.isEmpty()) return;
        ArcWaveSpawner.waveInfo thisWave = ArcWaveSpawner.arcWave.get(Math.min(state.wave - 1, ArcWaveSpawner.arcWave.size - 1));
        for(Tile tile : spawner.getSpawns()){
            if(Mathf.dst(tile.worldx(), tile.worldy(), Core.input.mouseWorldX(), Core.input.mouseWorldY()) < state.rules.dropZoneRadius){
                float curve = Mathf.curve(Time.time % 240f, 120f, 240f);
                Draw.z(Layer.effect - 2f);
                Draw.color(state.rules.waveTeam.color);
                Lines.stroke(4f);
                //flyer
                float flyerAngle = Angles.angle(world.width() / 2f, world.height() / 2f, tile.x, tile.y);
                float trns = Math.max(world.width(), world.height()) * Mathf.sqrt2 * tilesize;
                float spawnX = Mathf.clamp(world.width() * tilesize / 2f + Angles.trnsx(flyerAngle, trns), 0, world.width() * tilesize);
                float spawnY = Mathf.clamp(world.height() * tilesize / 2f + Angles.trnsy(flyerAngle, trns), 0, world.height() * tilesize);

                if(ArcWaveSpawner.hasFlyer){
                    Lines.line(tile.worldx(), tile.worldy(), spawnX, spawnY);
                    Tmp.v1.set(spawnX - tile.worldx(), spawnY - tile.worldy());
                    Tmp.v1.setLength(Tmp.v1.len() * curve);
                    Fill.circle(tile.worldx() + Tmp.v1.x, tile.worldy() + Tmp.v1.y, 8f);

                    Vec2 v = Core.camera.project(spawnX, spawnY);
                    flyerTable.setPosition(v.x, v.y);
                    flyerTable.table(Styles.black3, tt -> {
                        tt.add(FormatDefault.duration(state.wavetime / 60, false)).row();
                        thisWave.specLoc(tile.pos(), group -> group.type.flying);
                        tt.add(thisWave.proTable(false));
                        tt.row();
                        tt.add(thisWave.unitTable(tile.pos(), group -> group.type.flying)).maxWidth(mobile ? 400f : 750f).growX();
                    });
                }
                //ground

                if(curve > 0)
                    Lines.circle(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius * Interp.pow3Out.apply(curve));
                Lines.circle(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius);
                Lines.arc(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius - 3f, state.wavetime / state.rules.waveSpacing, 90f);
                float angle = Mathf.pi / 2 + state.wavetime / state.rules.waveSpacing * 2 * Mathf.pi;
                Draw.color(state.rules.waveTeam.color);
                Fill.circle(tile.worldx() + state.rules.dropZoneRadius * Mathf.cos(angle), tile.worldy() + state.rules.dropZoneRadius * Mathf.sin(angle), 8f);

                Vec2 v = Core.camera.project(tile.worldx(), tile.worldy());
                spawnerTable.setPosition(v.x, v.y);
                spawnerTable.table(Styles.black3, tt -> {
                    tt.add(FormatDefault.duration(state.wavetime / 60, false)).row();
                    thisWave.specLoc(tile.pos(), group -> !group.type.flying);
                    tt.add(thisWave.proTable(false));
                    tt.row();
                    tt.add(thisWave.unitTable(tile.pos(), group -> !group.type.flying)).maxWidth(mobile ? 400f : 750f).growX();
                });
                return;
            }
        }
    }

    private static boolean canAccept(Block block){
        if(block.group == BlockGroup.transportation) return true;
        for(Item item : content.items()){
            if(block.consumesItem(item) || block.itemCapacity > 0){
                return true;
            }
        }
        return false;
    }

    public static final Seq<Point> path = new Seq<>();

    public static void drawTransportFlow(){
        if(!enabled) return;

        //check tile being hovered over
        Tile hoverTile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if(hoverTile == null || hoverTile.build == null || !hoverTile.build.isDiscovered(player.team())){
            return;
        }

        path.clear();
        travelPath(new Point(hoverTile.build, null), ArcScanMode::getPrevious);
        drawPath(false);

        path.clear();
        travelPath(new Point(hoverTile.build, null), ArcScanMode::getNext);
        drawPath(true);
    }

    private static void travelPath(Point point, Func<Point, Seq<Point>> getNext){
        if(point.build == null || path.size > maxLoop) return;
        if(!point.trans) return;

        Point same = path.find(other -> point.build == other.build && (other.from == null || point.from.build == other.from.build));
        if(same != null){
            if(point.conduit >= same.conduit) return;
            else path.replace(same, point);
        }else path.add(point);

        getNext.get(point).each(p -> travelPath(p, getNext));
    }

    private static Seq<Point> getPrevious(Point point){
        Building build = point.build;
        if(build == null) return new Seq<>();
        Seq<Point> previous = new Seq<>();
        //质驱
        if(build instanceof MassDriver.MassDriverBuild){
            //暂时搞不定
        }//桥
        else if(build instanceof ItemBridge.ItemBridgeBuild bridge && !(build instanceof LiquidBridge.LiquidBridgeBuild)){
            bridge.incoming.each(pos -> previous.add(new Point(world.tile(pos).build, point)));
        }//导管桥
        else if(build instanceof DirectionBridge.DirectionBridgeBuild bridge){
            for(Building b : bridge.occupied){
                if(b != null){
                    previous.add(new Point(b, point));
                }
            }
        }
        for(Building b : build.proximity){
            Point from = new Point(b, b.relativeTo(build), b.block.instantTransfer ? point.conduit + 1 : 0, point);
            if(canInput(point, b, true) && canOutput(from, build, false)){
                previous.add(from);
            }else if(canOutput(from, build, false)){
                from.trans = false;
                previous.add(from);
            }
        }
        return previous;
    }

    private static Seq<Point> getNext(Point point){
        Building build = point.build;
        if(build == null) return new Seq<>();
        Seq<Point> next = new Seq<>();
        //质驱
        if(build instanceof MassDriver.MassDriverBuild massDriverBuild){
            if(massDriverBuild.arcLinkValid()){
                next.add(new Point(world.build(massDriverBuild.link), point));
            }
        }//桥
        else if(build instanceof ItemBridge.ItemBridgeBuild itemBridgeBuild && !(build instanceof LiquidBridge.LiquidBridgeBuild)){
            if(itemBridgeBuild.arcLinkValid()){
                next.add(new Point(world.build(itemBridgeBuild.link), point));
            }
        }//导管桥
        else if(build instanceof DirectionBridge.DirectionBridgeBuild directionBridgeBuild){
            DirectionBridge.DirectionBridgeBuild link = directionBridgeBuild.findLink();
            if(link != null){
                next.add(new Point(link, point));
            }
        }

        for(Building b : build.proximity){
            Point to = new Point(b, build.relativeTo(b), b.block.instantTransfer ? point.conduit + 1 : 0, point);
            if(canInput(to, build, false) && canOutput(point, b, true)){
                next.add(to);
            }else if(canInput(to, build, false)){
                to.trans = false;
                next.add(to);
            }
        }
        return next;
    }

    private static boolean canInput(Point point, Building from, boolean active){
        Building build = point.build;
        if(build == null || from == null) return false;
        if(from.block.instantTransfer && point.conduit > 2) return false;
        //装甲传送带
        if(build instanceof ArmoredConveyor.ArmoredConveyorBuild){
            return from != build.front() && (from instanceof Conveyor.ConveyorBuild || from == build.back());
        }//装甲导管
        else if(build instanceof Duct.DuctBuild ductBuild && ((Duct)ductBuild.block).armored){
            return from != build.front() && (from.block.isDuct || from == build.back());
        }//传送带和导管
        else if(build instanceof Conveyor.ConveyorBuild || build instanceof Duct.DuctBuild){
            return from != build.front();
        }//塑钢带
        else if(build instanceof StackConveyor.StackConveyorBuild stackConveyorBuild){
            return switch(stackConveyorBuild.state){
                case 2 -> from == build.back() && from instanceof StackConveyor.StackConveyorBuild;
                case 1 -> from != build.front();
                default -> from instanceof StackConveyor.StackConveyorBuild;
            };
        }//交叉器
        else if(build instanceof Junction.JunctionBuild){
            return point.facing == -1 || from.relativeTo(build) == point.facing;
        }//分类
        else if(build instanceof Sorter.SorterBuild sorterBuild){
            return !active || build.relativeTo(from) != point.facing && (sorterBuild.sortItem != null || (from.relativeTo(build) == point.facing) == ((Sorter)sorterBuild.block).invert);
        }//溢流
        else if(build instanceof OverflowGate.OverflowGateBuild){
            return !active || build.relativeTo(from) != point.facing;
        }//导管路由器与导管溢流
        else if(build instanceof DuctRouter.DuctRouterBuild || build instanceof OverflowDuct.OverflowDuctBuild){
            return from == build.back();
        }//桥
        else if(build instanceof ItemBridge.ItemBridgeBuild itemBridgeBuild){
            return itemBridgeBuild.arcCheckAccept(from);
        }//导管桥
        else if(build instanceof DirectionBridge.DirectionBridgeBuild directionBridgeBuild){
            return directionBridgeBuild.arcCheckAccept(from);
        }else if(build instanceof Router.RouterBuild){
            return true;
        }else if(canAccept(build.block)){
            point.trans = false;
            return true;
        }
        return false;
    }

    private static boolean canOutput(Point point, Building to, boolean active){
        Building build = point.build;
        if(build == null || to == null) return false;
        if(to.block.instantTransfer && point.conduit > 2) return false;
        //传送带和导管
        if(build instanceof Conveyor.ConveyorBuild || build instanceof Duct.DuctBuild){
            return to == build.front();
        }//塑钢带
        else if(build instanceof StackConveyor.StackConveyorBuild stackConveyor){
            if(stackConveyor.state == 2 && ((StackConveyor)stackConveyor.block).outputRouter){
                return to != build.back();
            }
            return to == build.front();
        }//交叉器
        else if(build instanceof Junction.JunctionBuild){
            return point.facing == -1 || build.relativeTo(to) == point.facing;
        }//分类
        else if(build instanceof Sorter.SorterBuild sorterBuild){
            return !active || to.relativeTo(build) != point.facing && (sorterBuild.sortItem != null || (build.relativeTo(to) == point.facing) == ((Sorter)sorterBuild.block).invert);
        }//溢流
        else if(build instanceof OverflowGate.OverflowGateBuild){
            return !active || to.relativeTo(build) != point.facing;
        }//导管路由器与导管溢流
        else if(build instanceof DuctRouter.DuctRouterBuild || build instanceof OverflowDuct.OverflowDuctBuild){
            return to != build.back();
        }//桥
        else if(build instanceof ItemBridge.ItemBridgeBuild bridge){
            return bridge.arcCheckDump(to);
        }//导管桥
        else if(build instanceof DirectionBridge.DirectionBridgeBuild directionBridgeBuild){
            DirectionBridge.DirectionBridgeBuild link = directionBridgeBuild.findLink();
            return link == null && build.relativeTo(to) == build.rotation;
        }else if(build instanceof Router.RouterBuild || build instanceof Unloader.UnloaderBuild){
            return true;
        }else if(build instanceof GenericCrafter.GenericCrafterBuild){
            point.trans = false;
            return true;
        }
        return false;
    }

    private static void drawPath(boolean forward){
        Color mainColor = forward ? Color.valueOf("80ff00") : Color.valueOf("ff8000");
        Color highlightColor = forward ? Color.valueOf("00cc00") : Color.red;
        path.each(p -> {
            if(p.from != null && p.trans){
                float x1 = p.build.tile.drawx(), y1 = p.build.tile.drawy();
                float x2 = p.from.build.tile.drawx(), y2 = p.from.build.tile.drawy();

                Draw.color(mainColor);
                Draw.color(Tmp.c1.set(mainColor).a(Mathf.absin(4f, 1f) * 0.4f + 0.6f));
                Lines.stroke(1.5f);
                Lines.line(x1, y1, x2, y2);
            }else{
                Drawf.selected(p.build, Tmp.c1.set(highlightColor).a(Mathf.absin(4f, 1f) * 0.5f + 0.5f));
            }
            Draw.reset();
        });
        path.each(p -> {
            if(p.from != null && p.trans){
                float x1 = p.build.tile.drawx(), y1 = p.build.tile.drawy();
                float x2 = p.from.build.tile.drawx(), y2 = p.from.build.tile.drawy();
                float dst = Mathf.dst(x1, y1, x2, y2);

                Draw.color(highlightColor);
                Fill.circle(x1, y1, 1.8f);

                if(dst > tilesize){
                    Draw.color(highlightColor);
                    Vec2 to = Tmp.v1.set(x1, y1), from = Tmp.v2.set(x2, y2);
                    if(!forward){
                        from.set(to);
                        to.set(x2, y2);
                    }
                    //在中间位置，按朝向方向绘制一个三角形
                    Vec2 v = to.div(from).scl(0.5f);
                    Fill.poly(v.x + from.x, v.y + from.y, 3, 3f, v.angle());
                }
            }
            Draw.reset();
        });
    }

    private static class Point{
        public final Building build;
        public byte facing = -1;
        public int conduit = 0;
        //用于记录端点方块
        public boolean trans = true;

        public final Point from;

        public Point(Building build, Point from){
            this.build = build;
            this.from = from;
        }

        public Point(Building build, byte facing, int conduit, Point from){
            this.build = build;
            this.facing = facing;
            this.conduit = conduit;
            this.from = from;
        }
    }
}
