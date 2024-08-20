package mindustryX.features.draw;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.meta.*;

// pick from https://github.com/MinRi2/MinerTools/blob/8ab2fe090cf24f0a5c8eaa0dcbea01f0a5447dd8/src/MinerTools/graphics/draw/player/PayloadDropHint.java
// 重制修正：WayZer
public class PayloadDropHint{
    public static void draw(Player player){
        var unit = player.unit() instanceof Payloadc ? (Unit & Payloadc)player.unit() : null;
        if(unit == null) return;
        Tile on = unit.tileOn();
        if(on == null) return;

        Draw.z(Layer.flyingUnit + 0.1f);
        //dropHint
        if(unit.payloads().any()){
            Payload payload = unit.payloads().peek();
            if(on.build != null && on.build.acceptPayload(on.build, payload)){
                draw(on.build, payload.content(), payload instanceof BuildPayload b ? b.build.rotation * 90 : payload.rotation() - 90);
            }else if(payload instanceof BuildPayload p){
                Building build = p.build;
                Block block = build.block;
                int tx = World.toTile(build.x - block.offset), ty = World.toTile(build.y - block.offset);
                boolean valid = Build.validPlace(block, build.team, tx, ty, build.rotation, false);

                Vec2 center = block.bounds(tx, ty, Tmp.r1).getCenter(Tmp.v1);
                draw(center, block, build.rotation * 90, valid);
            }else if(payload instanceof UnitPayload p){
                var u = p.unit;
                boolean valid = u.canPass(on.x, on.y) && Units.count(u.x, u.y, u.physicSize(), Flyingc::isGrounded) <= 1;
                draw(payload, payload.content(), u.rotation - 90, valid);
            }
        }
        //pickHint
        {
            Unit target = Units.closest(unit.team(), unit.x, unit.y, unit.type.hitSize * 2f, u -> u.isAI() && u.isGrounded() && unit.canPickup(u) && u.within(unit, u.hitSize + unit.hitSize));
            if(target != null){
                draw(target, target.type, target.rotation - 90);
                return;
            }
            Building build = on.build;
            if(build == null) return;
            Payload payload = build.getPayload();
            if(payload != null && unit.canPickupPayload(payload)){
                draw(payload, payload.content(), payload instanceof BuildPayload b ? b.build.rotation * 90 : payload.rotation());
                return;
            }
            if(build.block.buildVisibility != BuildVisibility.hidden && build.canPickup() && unit.canPickup(build)){
                draw(build, build.block, build.rotation * 90);
            }
        }
    }

    private static void draw(Position pos, UnlockableContent type, float rotation){
        draw(pos, type, rotation, true);
    }

    private static void draw(Position pos, UnlockableContent type, float rotation, boolean valid){
        Draw.color(!valid ? Color.red : Pal.accent, 0.6f);
        if(type instanceof Block block){
            float size = block.size * Vars.tilesize;
            Draw.rect(block.fullIcon, pos.getX(), pos.getY(), size, size, rotation);
            Lines.square(pos.getX(), pos.getY(), size * 0.9f, 20);
        }else if(type instanceof UnitType unit){
            Draw.rect(type.fullIcon, pos.getX(), pos.getY(), rotation);
            Lines.square(pos.getX(), pos.getY(), unit.hitSize, 20);
        }
        Draw.color();
    }
}