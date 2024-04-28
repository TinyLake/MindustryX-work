package mindustry.world.blocks.defense.turrets;

import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.weapons.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class PointDefenseTurret extends ReloadTurret{
    public final int timerTarget = timers++;
    public float retargetTime = 5f;

    public @Load(value = "@-base", fallback = "block-@size") TextureRegion baseRegion;

    public Color color = Color.white;
    public Effect beamEffect = Fx.pointBeam;
    public Effect hitEffect = Fx.pointHit;
    public Effect shootEffect = Fx.sparkShoot;

    public Sound shootSound = Sounds.lasershoot;

    public float shootCone = 5f;
    public float bulletDamage = 10f;
    public float shootLength = 3f;

    public PointDefenseTurret(String name){
        super(name);

        rotateSpeed = 20f;
        reload = 30f;

        coolantMultiplier = 2f;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{baseRegion, region};
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.reload, 60f / reload, StatUnit.perSecond);
        stats.add(Stat.damage, bulletDamage);
    }

    public class PointDefenseBuild extends ReloadTurretBuild{
        public @Nullable Bullet target;

        @Override
        public void updateTile(){

            //retarget
            if(timer(timerTarget, retargetTime)){
                target = PointDefenseWeapon.findEnemyBullet(team, x, y, range);
            }

            //pooled bullets
            if(target != null && !target.isAdded()){
                target = null;
            }

            if(coolant != null){
                updateCooling();
            }

            //look at target
            if(target != null && target.within(this, range) && target.team != team && target.type() != null && target.type().hittable){
                float dest = angleTo(target);
                rotation = Angles.moveToward(rotation, dest, rotateSpeed * edelta());
                reloadCounter += edelta();

                //shoot when possible
                if(Angles.within(rotation, dest, shootCone) && reloadCounter >= reload){
                    float realDamage = bulletDamage * state.rules.blockDamage(team);
                    if(target.damage() > realDamage){
                        target.damage(target.damage() - realDamage);
                    }else{
                        target.remove();
                    }

                    Tmp.v1.trns(rotation, shootLength);

                    beamEffect.at(x + Tmp.v1.x, y + Tmp.v1.y, rotation, color, new Vec2().set(target));
                    shootEffect.at(x + Tmp.v1.x, y + Tmp.v1.y, rotation, color);
                    hitEffect.at(target.x, target.y, color);
                    shootSound.at(x + Tmp.v1.x, y + Tmp.v1.y, Mathf.random(0.9f, 1.1f));
                    reloadCounter = 0;
                }
            }
        }

        @Override
        public boolean shouldConsume(){
            return super.shouldConsume() && target != null;
        }

        @Override
        public void draw(){
            Draw.rect(baseRegion, x, y);
            Drawf.shadow(region, x - (size / 2f), y - (size / 2f), rotation - 90);
            Draw.rect(region, x, y, rotation - 90);
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(rotation);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            rotation = read.f();
        }
    }
}
