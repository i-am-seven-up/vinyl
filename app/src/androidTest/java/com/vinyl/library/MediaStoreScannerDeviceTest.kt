package com.vinyl.library

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
internal class MediaStoreScannerDeviceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val createdUris = mutableListOf<Uri>()

    @Before fun grantAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName, permission)
    }

    @After fun removeOnlyTestMedia() {
        for (uri in createdUris) context.contentResolver.delete(uri, null, null)
    }

    @Test fun scansRealContentUrisAndFiltersShortAndSystemAudio() = runBlocking {
        val included = createWave("Đặng đi đâu", 31)
        createWave("Short", 10)
        createWave("Ringtone", 31, MediaStore.Audio.Media.IS_RINGTONE)
        createWave("Notification", 31, MediaStore.Audio.Media.IS_NOTIFICATION)
        createWave("Alarm", 31, MediaStore.Audio.Media.IS_ALARM)
        val songs = MediaStoreScanner(context).getSongs().filter { Uri.parse(it.contentUri) in createdUris }
        assertEquals(1, songs.size)
        assertEquals(included, Uri.parse(songs.single().contentUri))
        assertTrue(songs.single().titleNorm.startsWith("dang di dau"))
        assertEquals(31_000, songs.single().durationMs)
    }

    @Test fun deniedPermissionFailsInsteadOfReturningPartialOwnedMedia() {
        val deniedContext = object : ContextWrapper(context) {
            override fun checkSelfPermission(permission: String): Int = PackageManager.PERMISSION_DENIED
        }
        assertThrows(SecurityException::class.java) { runBlocking { MediaStoreScanner(deniedContext).getSongs() } }
    }

    @Test fun missingEmbeddedAndInaccessibleSiblingCoverReturnPlaceholderData() = runBlocking {
        val uri = createWave("No cover", 31)
        val store = ArtworkStore(context.contentResolver)
        assertNull(store.findArtwork(ArtworkRequest(uri.toString(), 1, "Music/VinylB1Tests/", "external_primary")))
        assertNull(store.findArtwork(ArtworkRequest("content://media/external_primary/audio/media/9223372036854775807", 1, null, "external_primary")))
    }

    private fun createWave(title: String, seconds: Int, systemFlag: String? = null): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$title-${System.nanoTime()}.wav")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            val folder = when (systemFlag) {
                MediaStore.Audio.Media.IS_RINGTONE -> "Ringtones"
                MediaStore.Audio.Media.IS_NOTIFICATION -> "Notifications"
                MediaStore.Audio.Media.IS_ALARM -> "Alarms"
                else -> "Music"
            }
            put(MediaStore.Audio.Media.RELATIVE_PATH, "$folder/VinylB1Tests/")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = checkNotNull(resolver.insert(collection, values))
        createdUris.add(uri)
        checkNotNull(resolver.openOutputStream(uri)).use { it.write(createWaveBytes(seconds)) }
        values.clear()
        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    private fun createWaveBytes(seconds: Int): ByteArray {
        val size = seconds * 8000 * 2
        val buffer = ByteBuffer.allocate(44 + size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + size).put("WAVEfmt ".toByteArray()).putInt(16)
        buffer.putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
        buffer.put("data".toByteArray()).putInt(size)
        for (index in 0 until size / 2) {
            buffer.putShort((kotlin.math.sin(index * 2 * Math.PI * 440 / 8000) * 1500).toInt().toShort())
        }
        return buffer.array()
    }
}
