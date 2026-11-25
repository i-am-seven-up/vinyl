package com.vinyl.library

import android.content.ContentResolver
import android.content.ContentUris
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer

internal data class ArtworkRequest(
    val songUri: String,
    val version: Long,
    val relativePath: String?,
    val volumeName: String,
)

internal class ArtworkStore(private val contentResolver: ContentResolver) {
    suspend fun findArtwork(request: ArtworkRequest): ByteArray? = withContext(Dispatchers.IO) {
        val embedded = try {
            val retriever = MediaMetadataRetriever()
            try {
                contentResolver.openAssetFileDescriptor(Uri.parse(request.songUri), "r")?.use { descriptor ->
                    if (descriptor.declaredLength >= 0) {
                        retriever.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.declaredLength)
                    } else {
                        retriever.setDataSource(descriptor.fileDescriptor)
                    }
                    retriever.embeddedPicture
                }
            } finally {
                retriever.release()
            }
        } catch (error: IOException) {
            Log.d("ArtworkStore", "Embedded cover unavailable", error)
            null
        } catch (error: RuntimeException) {
            Log.d("ArtworkStore", "Embedded cover unreadable", error)
            null
        }
        if (embedded != null) return@withContext embedded
        if (Build.VERSION.SDK_INT < 29 || request.relativePath == null) return@withContext null
        // Scoped storage may hide sibling images. Only open content URIs already accessible.
        try {
            val collection = MediaStore.Files.getContentUri(request.volumeName)
            contentResolver.query(
                collection, arrayOf(MediaStore.Files.FileColumns._ID),
                "relative_path = ? AND display_name = ?", arrayOf(request.relativePath, "cover.jpg"), null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val uri = ContentUris.withAppendedId(collection, cursor.getLong(0))
                    return@withContext contentResolver.openInputStream(uri)?.use { it.readCoverBytes() }
                }
            }
            null
        } catch (error: IOException) {
            Log.d("ArtworkStore", "Sibling cover unavailable", error)
            null
        } catch (error: RuntimeException) {
            Log.d("ArtworkStore", "Sibling cover inaccessible", error)
            null
        }
    }

    private fun java.io.InputStream.readCoverBytes(): ByteArray? {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) return output.toByteArray()
            if (output.size() + count > 8 * 1024 * 1024) return null
            output.write(buffer, 0, count)
        }
    }
}

internal class ArtworkKeyer : Keyer<ArtworkRequest> {
    override fun key(data: ArtworkRequest, options: Options): String = "${data.songUri}:${data.version}"
}

internal class ArtworkFetcherFactory(private val artworkStore: ArtworkStore) : Fetcher.Factory<ArtworkRequest> {
    override fun create(data: ArtworkRequest, options: Options, imageLoader: ImageLoader): Fetcher {
        return Fetcher {
            val bytes = artworkStore.findArtwork(data) ?: throw IOException("No readable album cover")
            SourceFetchResult(
                source = ImageSource(Buffer().write(bytes), options.fileSystem),
                mimeType = null,
                dataSource = DataSource.DISK,
            )
        }
    }
}
