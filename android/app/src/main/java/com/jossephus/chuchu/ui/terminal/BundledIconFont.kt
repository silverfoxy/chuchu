package com.jossephus.chuchu.ui.terminal

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.fonts.Font
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.jossephus.chuchu.R
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Draw private-use icons from the actual bundled font, without system font substitution. */
@RequiresApi(31)
internal class BundledIconFont(resources: Resources) {
    private val font = Font.Builder(resources, R.font.symbols_nerd_font_mono_regular).build()
    private val cmap = IconCmap(font.buffer)
    private val glyphs = HashMap<Int, IntArray>()
    private val position = FloatArray(2)
    private val bounds = RectF()

    fun contains(codepoint: Int): Boolean = isNerdFontPrivateUse(codepoint) && glyphId(codepoint) != 0

    private fun glyphId(codepoint: Int): Int =
        glyphs.getOrPut(codepoint) { intArrayOf(cmap.glyphId(codepoint)) }[0]

    fun draw(canvas: Canvas, codepoint: Int, x: Float, baseline: Float, paint: Paint) {
        position[0] = x
        position[1] = baseline
        canvas.drawGlyphs(glyphs.getValue(codepoint), 0, position, 0, 1, font, paint)
        // drawGlyphs does not apply text decorations as drawText does.
        if (paint.isUnderlineText) {
            val thickness = (paint.textSize / 16f).coerceAtLeast(1f)
            val y = baseline + thickness
            canvas.drawRect(x, y, x + font.getGlyphBounds(glyphId(codepoint), paint, bounds),
                y + thickness, paint)
        }
    }

    companion object {
        fun load(resources: Resources): BundledIconFont? {
            if (Build.VERSION.SDK_INT < 31) return null
            return runCatching { BundledIconFont(resources) }.getOrElse {
                Log.e("BundledIconFont", "Unable to load bundled icon glyphs", it)
                null
            }
        }
    }
}

/** The shipped font has a Unicode format-12 cmap covering both BMP and supplementary icons. */
internal class IconCmap(buffer: ByteBuffer) {
    private val data = buffer.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN)
    private val groups: Int
    private val count: Int

    init {
        val tableCount = data.getShort(4).toInt() and 0xffff
        val cmap = (0 until tableCount).map { 12 + it * 16 }
            .first { data.getInt(it) == 0x636d6170 }
            .let { data.getInt(it + 8) }
        val encodingCount = data.getShort(cmap + 2).toInt() and 0xffff
        val format12 = (0 until encodingCount).map { cmap + 4 + it * 8 }
            .filter { (data.getShort(it).toInt() and 0xffff) in listOf(0, 3) }
            .map { cmap + data.getInt(it + 4) }
            .first { data.getShort(it).toInt() == 12 }
        count = data.getInt(format12 + 12)
        groups = format12 + 16
        require(count >= 0 && groups.toLong() + count.toLong() * 12 <= data.limit())
    }

    fun glyphId(codepoint: Int): Int {
        var low = 0
        var high = count - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val offset = groups + mid * 12
            when {
                codepoint < data.getInt(offset) -> high = mid - 1
                codepoint > data.getInt(offset + 4) -> low = mid + 1
                else -> return data.getInt(offset + 8) + codepoint - data.getInt(offset)
            }
        }
        return 0
    }
}
