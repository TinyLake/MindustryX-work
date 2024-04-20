package mindustryX.features;

import arc.*;
import arc.util.*;
import mindustry.game.EventType.*;

public class DebugUtil{
    public static int lastDrawRequests = 0;
    public static long logicTime, rendererTime, uiTime;//nanos
    private static long rendererStart, uiStart;//nanos

    public static void init(){
        Events.run(Trigger.preDraw, () -> {
            lastDrawRequests = 0;
            rendererStart = Time.nanos();
        });
        Events.run(Trigger.postDraw, () -> rendererTime = Time.timeSinceNanos(rendererStart));
        Events.run(Trigger.uiDrawBegin, () -> uiStart = Time.nanos());
        Events.run(Trigger.uiDrawEnd, () -> uiTime = Time.timeSinceNanos(uiStart));
    }
}
