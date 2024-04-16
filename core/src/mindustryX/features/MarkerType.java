package mindustryX.features;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.arcModule.*;
import mindustry.arcModule.ui.dialogs.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustryX.features.ui.*;

import java.util.regex.*;

import static arc.graphics.g2d.Draw.color;
import static arc.graphics.g2d.Lines.stroke;
import static mindustry.Vars.*;

public class MarkerType{
    private static final Pattern posPattern = Pattern.compile("(?<type><[A-Za-z]+>)?\\((?<x>\\d+),(?<y>\\d+)\\)");
    /** 冷却时间 */
    public static final float heatTime = 60f;
    /** 滞留时间 */
    public static final float retainTime = 1800f;
    public static MarkerType mark, gatherMark, attackMark, defenseMark, quesMark;
    public static Seq<MarkerType> allTypes;
    public static MarkerType selected = mark;
    private static MarkElement last;

    public static void init(){
        mark = new MarkerType("Mark", new Effect(1800, e -> {
            color(e.color);
            stroke(2f);
            Lines.circle(e.x, e.y, 1f * tilesize);
            stroke(e.fout() * 1.5f + 0.5f);
            Lines.circle(e.x, e.y, 8f + e.finpow() * 92f);
            //Drawf.arrow(player.x, player.y, e.x, e.y, 5f * tilesize, 4f, Pal.command);
        }), Color.valueOf("eab678"));
        gatherMark = new MarkerType("Gather", new Effect(1800, e -> {
            color(e.color, 0.8f);
            stroke(2f);
            Lines.circle(e.x, e.y, 4f * tilesize);
            stroke(1f);
            Lines.circle(e.x, e.y, 4f * tilesize * (e.finpow() * 8f - (int)(e.finpow() * 8f)));
            for(int j = 0; j < 4; j++){
                if(e.fout() * 3 < j){
                    for(int i = 0; i < 8; i++){
                        float rot = i * 45f;
                        float radius = 4f * tilesize + j * 6f + 4f;
                        Drawf.simpleArrow(e.x + Angles.trnsx(rot, radius), e.y + Angles.trnsy(rot, radius), e.x, e.y, 4f, 2f, Color.cyan, Math.min(1f, j - e.fout() * 3));
                    }
                }
            }
        }), Color.cyan);
        attackMark = new MarkerType("Attack", new Effect(1800, e -> {
            color(e.color);
            stroke(2f);
            Lines.circle(e.x, e.y, 1f * tilesize);
            float radius = 20f + e.finpow() * 80f;
            Lines.circle(e.x, e.y, radius);
            for(int i = 0; i < 4; i++){
                float rot = i * 90f + 45f + (-Time.time) % 360f;
                Drawf.simpleArrow(e.x + Angles.trnsx(rot, radius), e.y + Angles.trnsy(rot, radius), e.x, e.y, 6f + 4 * e.finpow(), 2f + 4 * e.finpow());
            }
        }), Color.valueOf("#DC143C"));
        defenseMark = new MarkerType("Defend", new Effect(1800, e -> {
            color(Pal.heal);
            if(e.fin() < 0.2f){
                Lines.circle(e.x, e.y, 20f + e.fin() * 400f);
                return;
            }
            Lines.circle(e.x, e.y, 101f);
            Lines.circle(e.x, e.y, 93f);
            for(int i = 0; i < 16; i++){
                float rot = i * 22.5f;
                if((e.fin() - 0.2f) * 50 > i)
                    Drawf.simpleArrow(e.x, e.y, e.x + Angles.trnsx(rot, 120f), e.y + Angles.trnsy(rot, 120f), 96f, 4f);
            }
        }), Color.acid);
        quesMark = new MarkerType("What", new Effect(1200, e -> {
            color(Color.violet);
            stroke(2f);
            Draw.alpha(Math.min(e.fin() * 5, 1));
            Lines.arc(e.x, e.y + 25f, 10, 0.75f, 270);
            Lines.line(e.x, e.y + 15f, e.x, e.y + 7f);
            Lines.circle(e.x, e.y, 3f);
            Lines.circle(e.x, e.y + 18.5f, 27f);
        }), Color.pink);
        allTypes = Seq.with(mark, gatherMark, attackMark, defenseMark, quesMark);
    }

    static{
        init();
        selected = mark;
        Events.run(WorldLoadEvent.class, () -> last = null);
    }


    public final Color color;
    private final String name;
    private final Effect effect;
    public String localizedName;

    public MarkerType(String name, Effect effect, Color color){
        this.name = name;
        this.effect = effect;
        this.color = color;

        localizedName = Core.bundle.get("marker." + name + ".name", "unknown");
    }

    public String shortName(){
        return "[#" + color + "]" + localizedName.charAt(0);
    }

    public MarkElement at(Position pos){
        var element = new MarkElement(this, pos);
        element.show();
        return element;
    }

    public void markWithMessage(Vec2 pos){
        if(last != null && last.time < heatTime){
            Vars.ui.announce("请不要频繁标记!");
            return;
        }
        last = at(pos);
        UIExt.sendChatMessage(Strings.format("<ARCxMDTX>[#@]<@>[]@", color, name, FormatDefault.formatTile(pos)));
    }

    public static boolean resolveMessage(String text){
        var matcher = posPattern.matcher(Strings.stripColors(text));
        if(!matcher.find()) return false;
        var typeName = matcher.group("type");
        Vec2 pos = Tmp.v1.set(Strings.parseInt(matcher.group("x")), Strings.parseInt(matcher.group("y")));

        MarkerType type = mark;
        if(typeName != null){
            typeName = typeName.substring(1, typeName.length() - 1);
            for(var it : allTypes){
                if(it.name.equals(typeName))
                    type = it;
            }
        }else if(text.contains("集合")){
            type = gatherMark;
        }

        var exists = (MarkElement)Groups.draw.find(it -> it instanceof MarkElement e && e.message == null && e.within(pos.scl(tilesize), 2 * tilesize));
        last = exists != null ? exists : type.at(pos.scl(tilesize));
        last.message = text;
        MessageDialog.addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.markLoc, text, pos));
        return true;
    }

    public static void eachActive(Cons<MarkElement> cons){
        if(last == null) return;
        Groups.draw.each(d -> {
            if(d instanceof MarkElement e) cons.get(e);
        });
    }

    public static void lockOnLastMark(){
        if(last == null) return;
        control.input.panCamera(Tmp.v1.set(last));
        last.show();
    }

    public static @Nullable Position getLastPos(){
        return last;
    }


    public static class MarkElement extends EffectState{
        public final MarkerType type;
        @Nullable
        public String message;

        public MarkElement(MarkerType MarkerType, Position markPos){
            this.type = MarkerType;

            set(markPos);
            this.effect = MarkerType.effect;
            this.lifetime = effect.lifetime;
            this.color.set(MarkerType.color);
        }

        public void show(){
            time = 0;
            add();
        }

        @Override
        public void draw(){
            super.draw();
            showArrow();
            if(message != null)
                WorldLabel.drawAt(message, x, y, Layer.overlayUI, WorldLabel.flagOutline, 1);
        }

        private void showArrow(){
            Draw.reset();
            Drawf.arrow(player.x, player.y, x, y, 5f * tilesize, 4f, color);

            var p = Tmp.v1.set(this).sub(player).limit(4.5f * tilesize).add(player);
            DrawUtilities.drawText((int)(dst(player) / 8) + "", 0.2f, p.x, p.y, color, Align.center);
        }
    }
}
