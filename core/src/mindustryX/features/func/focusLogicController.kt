@file:JvmName("FuncX")
@file:JvmMultifileClass

package mindustryX.features.func

import arc.Core
import arc.util.Tmp
import mindustry.Vars
import mindustry.ai.types.LogicAI
import mindustry.entities.Units
import mindustryX.features.MarkerType
import mindustryX.features.RenderExt

fun focusLogicController() {
    val mouse = Core.input.mouseWorld()
    val logic = Units.closestOverlap(Vars.player.team(), mouse.x, mouse.y, 5f) { true }?.let { (it.controller() as? LogicAI)?.controller }
        ?: (if (RenderExt.showOtherInfo) Units.closestEnemy(Vars.player.team(), mouse.x, mouse.y, 5f) { true }?.let { (it.controller() as? LogicAI)?.controller } else null)
        ?: Core.input.mouseWorld().let { Vars.world.buildWorld(it.x, it.y)?.lastLogicController }
        ?: return
    Vars.control.input.panCamera(Tmp.v1.set(logic))
    MarkerType.mark.at(logic)
}