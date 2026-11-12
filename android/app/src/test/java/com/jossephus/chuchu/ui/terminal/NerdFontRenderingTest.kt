package com.jossephus.chuchu.ui.terminal

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import com.jossephus.chuchu.R
import com.jossephus.chuchu.ui.theme.ChuFontOption
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NerdFontRenderingTest {
    @Test
    fun `direct icon drawing ignores the system typeface`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val icons = checkNotNull(BundledIconFont.load(context.resources))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 64f; color = Color.WHITE }
        val expectedPaint = Paint(paint).apply {
            typeface = ResourcesCompat.getFont(context, R.font.symbols_nerd_font_mono_regular)
        }
        for (cp in listOf(0xE718, 0xF489, 0xF024B)) {
            assertTrue(icons.contains(cp))
            val actual = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
            val expected = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
            icons.draw(Canvas(actual), cp, 20f, 85f, paint)
            Canvas(expected).drawText(String(Character.toChars(cp)), 20f, 85f, expectedPaint)
            assertTrue("Direct U+${cp.toString(16)} must match bundled glyph", actual.sameAs(expected))
        }
        assertTrue(!icons.contains(0x4E2D)) // Real Chinese text must use normal shaping.
        assertTrue(!icons.contains(0xF8FF)) // Unassigned icons must retain the fallback path.
    }

    @Test
    fun `font cmap resolves both basic and supplementary private use icons`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val bytes = context.resources.openRawResource(R.font.symbols_nerd_font_mono_regular).use { it.readBytes() }
        val cmap = IconCmap(java.nio.ByteBuffer.wrap(bytes))
        assertEquals(681, cmap.glyphId(0xE718))
        assertEquals(3346, cmap.glyphId(0xF489))
        assertEquals(4103, cmap.glyphId(0xF024B))
        assertEquals(0, cmap.glyphId(0xF8FF))
    }

    @Test
    fun `bundled symbol font renders reported icons`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ResourcesCompat.getFont(context, R.font.symbols_nerd_font_mono_regular)
            textSize = 64f
            color = Color.WHITE
        }
        val bitmap = Bitmap.createBitmap(600, 120, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        for ((i, cp) in listOf(0xE718, 0xF489, 0xF024B).withIndex()) {
            val glyph = String(Character.toChars(cp))
            assertTrue("Bundled font must contain U+${cp.toString(16)}", paint.hasGlyph(glyph))
            for (font in ChuFontOption.entries) {
                val primary = Paint().apply {
                    typeface = ResourcesCompat.getFont(context, font.regularFontResId)
                }
                assertEquals("${font.label}: single U+${cp.toString(16)}", PAINT_SYMBOLS,
                    pickPaintChoice(cp, hashMapOf(), hashMapOf(), primary, paint, Paint(), Paint()))
                assertEquals("${font.label}: cluster U+${cp.toString(16)}", PAINT_SYMBOLS,
                    pickPaintChoice(glyph, hashMapOf(), primary, paint, Paint(), Paint()))
            }
            canvas.drawText(glyph, 30f + i * 180f, 85f, paint)
            val pixels = IntArray(120 * 120)
            bitmap.getPixels(pixels, 0, 120, i * 180, 0, 120, 120)
            assertTrue("U+${cp.toString(16)} must draw visible pixels", pixels.any { it != Color.BLACK })
        }
        val output = File("build/outputs/nerd-font-icons.png")
        checkNotNull(output.parentFile).mkdirs()
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
