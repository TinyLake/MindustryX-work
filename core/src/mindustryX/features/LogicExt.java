package mindustryX.features;

import arc.*;
import mindustry.game.EventType.*;

public class LogicExt{
    public static void init(){
        Events.run(Trigger.update, () -> {
        });
    }
}