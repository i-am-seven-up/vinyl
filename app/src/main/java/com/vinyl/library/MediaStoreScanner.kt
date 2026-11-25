package com.vinyl.library

import android.content.ContentResolver
import android.content.ContentUris
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import com.vinyl.shared.database.Song
import java.io.IOException
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal class MediaStoreScanner(private val context: Context) {
    private val contentResolver: ContentResolver = context.contentResolver

    suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
        requireAudioPermission()
        val audio = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = mutableListOf(
            "_id", "album_id", "artist_id", "title", "artist", "album", "duration",
            "track", "date_modified", "is_ringtone", "is_notification", "is_alarm",
        )
        if (Build.VERSION.SDK_INT >= 29) {
            projection.add(MediaStore.MediaColumns.RELATIVE_PATH)
            projection.add(MediaStore.MediaColumns.VOLUME_NAME)
        }
        val songs = mutableListOf<Song>()
        val cursor = contentResolver.query(audio, projection.toTypedArray(), null, null, null)
            ?: throw IOException("MediaStore returned no cursor")
        cursor.use {
            // One batched query; projection positions are fixed above.
            while (it.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val durationMs = it.getLong(6)
                if (!isLibrarySong(durationMs, it.getInt(9) != 0, it.getInt(10) != 0, it.getInt(11) != 0)) continue
                val volume = if (Build.VERSION.SDK_INT >= 29) it.getString(13) ?: MediaStore.VOLUME_EXTERNAL_PRIMARY else "external"
                val collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.getContentUri(volume) else audio
                val title = it.getString(3).orEmpty()
                val artist = it.getString(4).orEmpty()
                val album = it.getString(5).orEmpty()
                songs.add(Song(
                    contentUri = ContentUris.withAppendedId(collection, it.getLong(0)).toString(),
                    albumId = "$volume:${it.getLong(1)}",
                    artistId = "$volume:${it.getLong(2)}",
                    title = title, artist = artist, album = album,
                    titleNorm = normalizeSearchText(title), artistNorm = normalizeSearchText(artist),
                    albumNorm = normalizeSearchText(album), durationMs = durationMs,
                    trackNumber = it.getInt(7), dateModified = it.getLong(8),
                    relativePath = if (Build.VERSION.SDK_INT >= 29) it.getString(12) else null,
                    volumeName = volume,
                ))
            }
        }
        requireAudioPermission()
        songs
    }

    private fun requireAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE
        if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Audio permission is required for a complete library scan")
        }
    }
}

internal fun isLibrarySong(durationMs: Long, isRingtone: Boolean, isNotification: Boolean, isAlarm: Boolean): Boolean {
    return durationMs >= 30_000 && !isRingtone && !isNotification && !isAlarm
}

internal fun normalizeSearchText(text: String): String {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace('đ', 'd').replace('Đ', 'D').lowercase(Locale.ROOT)
}
