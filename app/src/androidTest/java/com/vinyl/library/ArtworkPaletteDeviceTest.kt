package com.vinyl.library

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

internal class ArtworkPaletteDeviceTest {
    @Test fun cachesColorsAndMissingCoverByUriAndVersion() = runBlocking {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(30, 100, 200))
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        var reads = 0
        val palette = ArtworkPalette { request -> reads++; if (request.songUri == "missing") null else output.toByteArray() }
        val request = ArtworkRequest("cover", 1, null, "external")
        val colors = palette.findColors(request)
        assertNotNull(colors)
        assertEquals(colors, palette.findColors(request))
        assertEquals(1, reads)
        assertNotNull(palette.findColors(request.copy(version = 2)))
        assertEquals(2, reads)
        val missing = request.copy(songUri = "missing")
        assertNull(palette.findColors(missing))
        assertNull(palette.findColors(missing))
        assertEquals(3, reads)
        assertNull(ArtworkPalette { byteArrayOf(0, 1, 2) }.findColors(request))
    }
}
