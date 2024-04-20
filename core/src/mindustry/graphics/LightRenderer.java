package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.*;

import static mindustry.Vars.*;

/** Renders overlay lights. Client only. */
public class LightRenderer{
    private static final int scaling = 4;

    private final FrameBuffer buffer = new FrameBuffer();
    private final Seq<Runnable> lights = new Seq<>();

    public void add(Runnable run){
        if(!enabled()) return;

        lights.add(run);
    }

    public void add(float x, float y, float radius, Color color, float opacity){
        if(!enabled() || radius <= 0f) return;

        float res = Color.toFloatBits(color.r, color.g, color.b, opacity);
        lights.add(CircleLight.pool.obtain().set(x, y, res, radius));
    }

    public void add(float x, float y, TextureRegion region, Color color, float opacity){
        add(x, y, region, 0f, color, opacity);
    }

    public void add(float x, float y, TextureRegion region, float rotation, Color color, float opacity){
        if(!enabled()) return;

        float res = color.toFloatBits();
        float xscl = Draw.xscl, yscl = Draw.yscl;
        add(RegionData.pool.obtain().set(res, opacity, xscl, yscl, region, x, y, rotation));
    }

    public void line(float x, float y, float x2, float y2, float stroke, Color tint, float alpha){
        if(!enabled()) return;

        add(LineData.pool.obtain().set(tint, alpha, x2, x, y2, y, stroke));
    }

    public boolean enabled(){
        return state.rules.lighting && state.rules.ambientLight.a > 0.0001f && renderer.drawLight;
    }

    @SuppressWarnings("unchecked")
    public void draw(){
        if(!Vars.enableLight){
            if(lights.isEmpty())return;
            Pools.freeAll(lights);
            lights.clear();
            return;
        }

        buffer.resize(Core.graphics.getWidth()/scaling, Core.graphics.getHeight()/scaling);

        Draw.color();
        buffer.begin(Color.clear);
        Draw.sort(false);
        Gl.blendEquationSeparate(Gl.funcAdd, Gl.max);
        //apparently necessary
        Blending.normal.apply();

        for(Runnable run : lights){
            run.run();
        }
        Draw.reset();
        Draw.sort(true);
        buffer.end();
        Gl.blendEquationSeparate(Gl.funcAdd, Gl.funcAdd);

        Draw.color();
        Shaders.light.ambient.set(state.rules.ambientLight);
        buffer.blit(Shaders.light);

        Pools.freeAll(lights);
        lights.clear();
    }

    static class CircleLight implements Runnable, Pool.Poolable{
        static Pool<CircleLight> pool = Pools.get(CircleLight.class,CircleLight::new);
        private static TextureRegion circleRegion;
        float x, y, color, radius;

        @Override
        public void reset(){}

        public CircleLight set(float x, float y, float color, float radius){
            if(circleRegion == null) circleRegion = Core.atlas.find("circle-shadow");
            this.x = x;
            this.y = y;
            this.color = color;
            this.radius = radius;
            return this;
        }

        @Override
        public void run(){
            Draw.color(color);
            Draw.rect(circleRegion, x, y, radius * 2, radius * 2);
        }
    }

    private static class RegionData implements Runnable, Pool.Poolable{
        static Pool<RegionData> pool = Pools.get(RegionData.class,RegionData::new);
        private float res;
        private float opacity;
        private float xscl;
        private float yscl;
        private TextureRegion region;
        private float x;
        private float y;
        private float rotation;

        @Override
        public void reset(){
            region = null;
        }

        public RegionData set(float res, float opacity, float xscl, float yscl, TextureRegion region, float x, float y, float rotation){
            this.res = res;
            this.opacity = opacity;
            this.xscl = xscl;
            this.yscl = yscl;
            this.region = region;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            return this;
        }

        @Override
        public void run(){
            Draw.color(res);
            Draw.alpha(opacity);
            Draw.scl(xscl, yscl);
            Draw.rect(region, x, y, rotation);
            Draw.scl();
        }
    }

    private static class LineData implements Runnable, Pool.Poolable{
        static Pool<LineData> pool = Pools.get(LineData.class,LineData::new);
        private static final float[] vertices = new float[24];
        private static TextureRegion ledge,lmid;
        private Color tint;
        private float alpha;
        private float x2;
        private float x;
        private float y2;
        private float y;
        private float stroke;

        @Override
        public void reset(){}

        public LineData set(Color tint, float alpha, float x2, float x, float y2, float y, float stroke){
            if(ledge == null){
                ledge = Core.atlas.find("circle-end");
                lmid = Core.atlas.find("circle-mid");
            }
            this.tint = tint;
            this.alpha = alpha;
            this.x2 = x2;
            this.x = x;
            this.y2 = y2;
            this.y = y;
            this.stroke = stroke;
            return this;
        }

        @Override
        public void run(){
            Draw.color(tint, alpha);

            float rot = Mathf.angleExact(x2 - x, y2 - y);
            float color = Draw.getColor().toFloatBits();
            float u = lmid.u;
            float v = lmid.v2;
            float u2 = lmid.u2;
            float v2 = lmid.v;

            Vec2 v1 = Tmp.v1.trnsExact(rot + 90f, stroke);
            float lx1 = x - v1.x, ly1 = y - v1.y,
            lx2 = x + v1.x, ly2 = y + v1.y,
            lx3 = x2 + v1.x, ly3 = y2 + v1.y,
            lx4 = x2 - v1.x, ly4 = y2 - v1.y;

            vertices[0] = lx1;
            vertices[1] = ly1;
            vertices[2] = color;
            vertices[3] = u;
            vertices[4] = v;
            vertices[5] = 0;

            vertices[6] = lx2;
            vertices[7] = ly2;
            vertices[8] = color;
            vertices[9] = u;
            vertices[10] = v2;
            vertices[11] = 0;

            vertices[12] = lx3;
            vertices[13] = ly3;
            vertices[14] = color;
            vertices[15] = u2;
            vertices[16] = v2;
            vertices[17] = 0;

            vertices[18] = lx4;
            vertices[19] = ly4;
            vertices[20] = color;
            vertices[21] = u2;
            vertices[22] = v;
            vertices[23] = 0;

            Draw.vert(ledge.texture, vertices, 0, vertices.length);

            Vec2 v3 = Tmp.v2.trnsExact(rot, stroke);

            u = ledge.u;
            v = ledge.v2;
            u2 = ledge.u2;
            v2 = ledge.v;

            vertices[0] = lx4;
            vertices[1] = ly4;
            vertices[2] = color;
            vertices[3] = u;
            vertices[4] = v;
            vertices[5] = 0;

            vertices[6] = lx3;
            vertices[7] = ly3;
            vertices[8] = color;
            vertices[9] = u;
            vertices[10] = v2;
            vertices[11] = 0;

            vertices[12] = lx3 + v3.x;
            vertices[13] = ly3 + v3.y;
            vertices[14] = color;
            vertices[15] = u2;
            vertices[16] = v2;
            vertices[17] = 0;

            vertices[18] = lx4 + v3.x;
            vertices[19] = ly4 + v3.y;
            vertices[20] = color;
            vertices[21] = u2;
            vertices[22] = v;
            vertices[23] = 0;

            Draw.vert(ledge.texture, vertices, 0, vertices.length);

            vertices[0] = lx2;
            vertices[1] = ly2;
            vertices[2] = color;
            vertices[3] = u;
            vertices[4] = v;
            vertices[5] = 0;

            vertices[6] = lx1;
            vertices[7] = ly1;
            vertices[8] = color;
            vertices[9] = u;
            vertices[10] = v2;
            vertices[11] = 0;

            vertices[12] = lx1 - v3.x;
            vertices[13] = ly1 - v3.y;
            vertices[14] = color;
            vertices[15] = u2;
            vertices[16] = v2;
            vertices[17] = 0;

            vertices[18] = lx2 - v3.x;
            vertices[19] = ly2 - v3.y;
            vertices[20] = color;
            vertices[21] = u2;
            vertices[22] = v;
            vertices[23] = 0;

            Draw.vert(ledge.texture, vertices, 0, vertices.length);
        }
    }
}
