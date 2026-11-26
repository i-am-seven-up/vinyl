package com.vinyl.library

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class ArtworkColors(val background: Int, val foreground: Int)
private data class CachedColors(val colors: ArtworkColors?)

internal class ArtworkPalette(private val findArtwork: suspend (ArtworkRequest) -> ByteArray?) {
    private val cache = LruCache<ArtworkRequest, CachedColors>(64)
    private val mutex = Mutex()

    suspend fun findColors(request: ArtworkRequest): ArtworkColors? = withContext(Dispatchers.IO) {
        mutex.withLock {
            cache.get(request)?.let { return@withLock it.colors }
            val bytes = findArtwork(request)
            val colors = if (bytes == null) null else calculateColors(bytes)
            cache.put(request, CachedColors(colors))
            colors
        }
    }

    private fun calculateColors(bytes: ByteArray): ArtworkColors? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = 1 }
        while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 128) options.inSampleSize *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        return try {
            val swatch = Palette.from(bitmap).generate().dominantSwatch
            if (swatch == null) null else ArtworkColors(swatch.rgb, swatch.bodyTextColor)
        } finally { bitmap.recycle() }
    }
}
