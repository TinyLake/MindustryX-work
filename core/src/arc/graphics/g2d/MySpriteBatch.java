package arc.graphics.g2d;

import arc.graphics.*;
import mindustryX.features.*;

//MDTX: add some DebugUtil count.
public class MySpriteBatch extends SpriteBatch{
    @Override
    protected void flush(){
        DebugUtil.lastFlushCount++;
        super.flush();
    }

    @Override
    protected void flushRequests(){
        DebugUtil.lastVertices += requestVertOffset / 6;
        DebugUtil.lastDrawRequests += numRequests;
        super.flushRequests();
    }

    @Override
    protected void switchTexture(Texture texture){
        DebugUtil.lastSwitchTexture++;
        super.switchTexture(texture);
    }
}
