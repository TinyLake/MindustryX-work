@file:JvmName("FuncX")
@file:JvmMultifileClass

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
    fontScl: Float = 1f, color: Color = Color.white, anchor: Int = Align.center,
    font: Font = Fonts.outline, background: Boolean = false,
) {
    val p = Tmp.v1.set(pos)
    //参考来源 mindustry.gen.WorldLabel.drawAt
    val z = Drawf.text()
    val ints = font.usesIntegerPositions()
    font.setUseIntegerPositions(false)
    font.data.setScale(0.25f / Scl.scl() * fontScl)
    font.color = color

    Pools.obtain(GlyphLayout::class.java, ::GlyphLayout).apply {
        setText(font, text)

        if (Align.isCenterVertical(anchor)) p.y += height / 2
        else if (Align.isBottom(anchor)) p.y += height
        var centerX = p.x
        if (Align.isLeft(anchor)) centerX += width / 2
        else if (Align.isRight(anchor)) centerX -= width / 2
        if (background) {
            Draw.color(Color.black, 0.3f)
            Fill.rect(centerX, p.y - height / 2, width + 2, height + 3)
            Draw.color()
        }
    }.let(Pools::free)
    val align = if (Align.isCenterHorizontal(anchor)) anchor or Align.center else anchor
    //此次x为绘制区域顶部
    font.draw(text, p.x, p.y, 0f, align, false)
    Draw.reset()

    font.color.set(Color.white)
    font.data.setScale(1f)
    font.setUseIntegerPositions(ints)
    Draw.z(z)
}