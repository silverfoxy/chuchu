package com.jossephus.chuchu.ui.terminal

import android.graphics.Paint
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.jossephus.chuchu.ui.theme.ChuTheme
import org.junit.Rule
import androidx.core.content.res.ResourcesCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jossephus.chuchu.R
import com.jossephus.chuchu.service.terminal.GhosttyBridge
import com.jossephus.chuchu.service.terminal.TerminalSnapshot
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NerdFontDeviceTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun canvasRendersReportedIcons() {
        val text = "\uE718 \uF489 " + String(Character.toChars(0xF024B)) + " v26.13.0"
        val cells = text.codePoints().toArray()
        val snapshot = TerminalSnapshot(
            cols = cells.size, rows = 1, cursorX = 0, cursorY = 0, cursorVisible = false,
            defaultBgArgb = Color.BLACK, defaultFgArgb = Color.WHITE,
            codepoints = cells, fgArgb = IntArray(cells.size) { Color.WHITE },
            bgArgb = IntArray(cells.size) { Color.BLACK }, flags = ByteArray(cells.size),
        )
        composeRule.setContent {
            ChuTheme {
                TerminalCanvas(snapshot, Modifier.size(360.dp, 80.dp).testTag("icons"),
                    fontSizeSp = 24f, enableGestures = false)
            }
        }
        val bitmap = composeRule.onNodeWithTag("icons").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.openFileOutput("nerd-font-canvas.png", 0).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun bundledFontContainsReportedIconsOnDevice() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val paint = Paint().apply {
            typeface = ResourcesCompat.getFont(context, R.font.symbols_nerd_font_mono_regular)
        }
        for (cp in listOf(0xE718, 0xF489, 0xF024B)) {
            assertTrue("Bundled font missing U+${cp.toString(16)}",
                paint.hasGlyph(String(Character.toChars(cp))))
        }
    }

    @Test
    fun nativeTerminalPreservesPrivateUseCodepoints() {
        val bridge = GhosttyBridge()
        assertTrue(bridge.nativeStatus(), bridge.isLoaded())
        val handle = bridge.nativeCreate(20, 2, 0)
        assertTrue(handle != 0L)
        try {
            val codepoints = intArrayOf(0xE718, 0xF489, 0xF024B)
            val text = String(codepoints, 0, codepoints.size)
            bridge.nativeWriteRemote(handle, text.toByteArray(Charsets.UTF_8))
            val snapshot = TerminalSnapshot.fromByteBuffer(bridge.nativeSnapshot(handle))
            assertArrayEquals(codepoints, snapshot.codepoints.take(3).toIntArray())
        } finally {
            bridge.nativeDestroy(handle)
        }
    }
}
