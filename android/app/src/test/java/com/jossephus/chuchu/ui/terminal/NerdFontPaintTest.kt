package com.jossephus.chuchu.ui.terminal

import android.graphics.Paint
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NerdFontPaintTest {
    private class GlyphPaint(private val supported: Boolean) : Paint() {
        override fun hasGlyph(string: String): Boolean = supported
    }

    @Test
    fun `bundled icons win even when Android fallback claims the glyph`() {
        for (codepoint in listOf(0xE718, 0xF489, 0xF024B, 0x100000)) {
            val glyph = String(Character.toChars(codepoint))
            assertEquals(PAINT_SYMBOLS, pickPaintChoice(
                glyph, hashMapOf(), GlyphPaint(true), GlyphPaint(true),
                GlyphPaint(true), GlyphPaint(true),
            ))
            assertEquals(PAINT_SYMBOLS, pickPaintChoice(
                codepoint, hashMapOf(), hashMapOf(), GlyphPaint(true), GlyphPaint(true),
                GlyphPaint(true), GlyphPaint(true),
            ))
        }
    }

    @Test
    fun `ordinary text and missing bundled icons retain primary font`() {
        for ((glyph, symbolsAvailable) in listOf("A" to true, "中" to true, "\uE718" to false)) {
            assertEquals(PAINT_PRIMARY, pickPaintChoice(
                glyph, hashMapOf(), GlyphPaint(true), GlyphPaint(symbolsAvailable),
                GlyphPaint(true), GlyphPaint(true),
            ))
            assertEquals(PAINT_PRIMARY, pickPaintChoice(
                glyph.codePointAt(0), hashMapOf(), hashMapOf(), GlyphPaint(true),
                GlyphPaint(symbolsAvailable), GlyphPaint(true), GlyphPaint(true),
            ))
        }
    }
}
