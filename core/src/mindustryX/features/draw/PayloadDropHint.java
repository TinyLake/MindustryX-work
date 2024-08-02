package mindustryX.features.draw;

import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;

// pick from https://github.com/MinRi2/MinerTools/blob/8ab2fe090cf24f0a5c8eaa0dcbea01f0a5447dd8/src/MinerTools/graphics/draw/player/PayloadDropHint.java
public class PayloadDropHint{
    public static void draw(Player player){
        var unit = player.unit() instanceof Payloadc ? (Unit & Payloadc)player.unit() : null;
        if(unit == null) return;

        Draw.z(Layer.flyingUnit + 0.1f);
        if(unit.payloads().any()){
            Payload payload = unit.payloads().peek();
            if(payload instanceof BuildPayload p){
                buildDropHint(unit, p);
            }else if(payload instanceof UnitPayload p){
                unitDropHint(unit, p);
            }
        }

        if(!unitPickUpHint(unit)){
            buildPickUpHint(unit);
        }
    }

    private static void buildPickUpHint(Payloadc unit){
        Tile tile = unit.tileOn();
        if(tile == null) return;
        Building build = tile.build;
        if(build == null || !unit.canPickup(build)) return;

        Block block = build.block;
        float size = block.size * Vars.tilesize;

        Draw.color(Pal.accent, 0.6f);
        Draw.rect(block.fullIcon, build.x, build.y, size, size, build.rotation * 90);
        Lines.square(build.x, build.y, size * 0.9f, 20);
        Draw.color();
    }

    private static <T extends Unit&Payloadc> boolean unitPickUpHint(T unit){
        Unit target = Units.closest(unit.team(), unit.x, unit.y, unit.type.hitSize * 2f, u -> u.isAI() && u.isGrounded() && unit.canPickup(u) && u.within(unit, u.hitSize + unit.hitSize));
        if(target == null) return false;
        Draw.color(Pal.accent, 0.6f);
        Draw.rect(target.type.fullIcon, target.x, target.y, target.rotation - 90);
        Lines.square(target.x, target.y, target.type.hitSize, 20);
        Draw.color();
        return true;
    }

    private static void buildDropHint(Unit unit, BuildPayload payload){
        Building build = payload.build;
        Block block = build.block;
        Tile on = Vars.world.tileWorld(unit.x - block.offset, unit.y - block.offset);
        if(on == null) return;

        boolean valid = Build.validPlace(block, build.team, on.x, on.y, build.rotation, false);
        float size = block.size * Vars.tilesize;
        int rot = block.rotate ? (int)((unit.rotation + 45f) / 90f) % 4 * 90 : 0;

        Draw.color(!valid ? Color.red : Pal.accent, 0.6f);
        Draw.rect(block.fullIcon, on.x * Vars.tilesize, on.y * Vars.tilesize, size, size, rot);
        Draw.color();
    }

    private static void unitDropHint(Unit unit, UnitPayload payload){
        Unit u = payload.unit;
        boolean valid = u.canPass(unit.tileX(), unit.tileY()) && Units.count(unit.x, unit.y, u.physicSize(), Flyingc::isGrounded) <= 1;

        Draw.color(!valid ? Color.red : Pal.accent, 0.6f);
        Draw.rect(u.type.fullIcon, unit.x, unit.y, unit.rotation - 90);
        Draw.color();
    }
}