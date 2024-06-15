@file:JvmName("FuncX")

package mindustryX.features.func

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.Font
import arc.graphics.g2d.GlyphLayout
import arc.math.geom.Position
import arc.scene.ui.layout.Scl
import arc.util.Align
import arc.util.Tmp
import arc.util.pooling.Pools
import mindustry.graphics.Drawf
import mindustry.ui.Fonts

/**
 * 绘制文字
 * @param fontScl 字体大小，相对世界尺寸，约一格方块大小。如果要按UI绘制，使用[Scl.scl]
 */
@JvmOverloads
fun drawText(
    pos: Position, text: String,
    fontScl: Float = 1f, color: Color = Color.white, align: Int = Align.center,
    font: Font = Fonts.outline, background: Boolean = false,
) {
    Tmp.v1.set(pos)
    val x = Tmp.v1.x
    var y = Tmp.v1.y
    //参考来源 mindustry.gen.WorldLabel.drawAt
    val z = Drawf.text()
    val ints = font.usesIntegerPositions()
    font.setUseIntegerPositions(false)
    font.data.setScale(0.25f / Scl.scl() * fontScl)
    font.color = color

    Pools.obtain(GlyphLayout::class.java, ::GlyphLayout).apply {
        setText(font, text)
        if (Align.isTop(align)) y += height
        else if (Align.isCenterVertical(align)) y += height / 2
        if (background) {
            Draw.color(Color.black, 0.3f)
            Fill.rect(x, y - height / 2, width + 2, height + 3)
            Draw.color()
        }
    }.let(Pools::free)
    font.draw(text, pos.x, pos.y, 0f, Align.center, false)
    Draw.reset()

    font.color.set(Color.white)
    font.data.setScale(1f)
    font.setUseIntegerPositions(ints)
    Draw.z(z)
}