package com.jossephus.chuchu.ui.theme

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PatchedTerminalFontTest {
    @Test
    fun `primary JetBrains font renders icons with one terminal cell advance`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val font = ResourcesCompat.getFont(context, ChuFontOption.JetBrainsMono.regularFontResId)
        assertNotNull(font)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = 64f
            color = Color.WHITE
        }
        val cellWidth = paint.measureText("M")
        assertEquals(cellWidth, paint.measureText("i"), 0.01f)
        for (cp in listOf(0xE718, 0xF489, 0xF024B)) {
            val glyph = String(Character.toChars(cp))
            assertTrue("Primary font must contain U+${cp.toString(16)}", paint.hasGlyph(glyph))
            assertEquals("Icon advance must fit one cell", cellWidth, paint.measureText(glyph), 0.01f)
            val bitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawText(glyph, 20f, 85f, paint)
            val pixels = IntArray(120 * 120)
            bitmap.getPixels(pixels, 0, 120, 0, 0, 120, 120)
            assertTrue("U+${cp.toString(16)} must be visible", pixels.any { (it ushr 24) != 0 })
        }
    }
}
