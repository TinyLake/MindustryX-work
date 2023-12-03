package mindustryX.events;

import mindustry.game.*;
import mindustry.gen.*;
import mindustryX.*;

/** Called after the team of a player changed. */
@MindustryXApi
public class PlayerTeamChangedEvent{
    public final Team previous;
    public final Player player;
    public PlayerTeamChangedEvent(Team previous, Player player) {
        this.previous = previous;
        this.player = player;
    }
}

