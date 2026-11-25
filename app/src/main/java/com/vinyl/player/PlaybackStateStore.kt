package com.vinyl.player

import android.content.ContentResolver
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.vinyl.shared.database.PlaybackDao
import com.vinyl.shared.database.PlaybackQueueItem
import com.vinyl.shared.database.SavedPlayback
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PlaybackStateStore(private val dao: PlaybackDao, private val resolver: ContentResolver) {
    suspend fun findPlayback(): SavedPlayback? = withContext(Dispatchers.IO) {
        val saved = dao.findPlayback() ?: return@withContext null
        filterPlayback(saved) { item ->
            val uri = Uri.parse(item.uri)
            // Only local MediaStore audio/content and app files are restored, never network input.
            if (uri.scheme != "content" && uri.scheme != "file") false else {
                try { resolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false }
                catch (_: SecurityException) { false }
                catch (_: IOException) { false }
            }
        }
    }
}

internal fun filterPlayback(saved: SavedPlayback, canRead: (PlaybackQueueItem) -> Boolean): SavedPlayback {
    val retained = saved.queue.withIndex().filter { canRead(it.value) }
    val oldIndex = saved.snapshot.currentIndex
    val exactIndex = retained.indexOfFirst { it.index == oldIndex }
    val newIndex = if (exactIndex >= 0) exactIndex else retained.indexOfFirst { it.index > oldIndex }.let { if (it >= 0) it else retained.lastIndex }
    return SavedPlayback(
        saved.snapshot.copy(currentIndex = newIndex.coerceAtLeast(0), positionMs = if (exactIndex >= 0) saved.snapshot.positionMs.coerceAtLeast(0) else 0),
        retained.mapIndexed { index, entry -> entry.value.copy(position = index) },
    )
}

internal fun PlaybackQueueItem.toMediaItem(): MediaItem = MediaItem.Builder().setMediaId(mediaId).setUri(uri)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).build()).build()
