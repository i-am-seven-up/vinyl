package com.vinyl.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.vinyl.MainActivity
import com.vinyl.VinylApplication
import com.vinyl.shared.database.PlaybackQueueItem
import com.vinyl.shared.database.PlaybackSnapshot
import com.vinyl.shared.database.PlayEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

// Android instantiates this manifest service; it owns the only player.
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private val writes = Channel<PlaybackCheckpoint>(Channel.UNLIMITED)
    private val restored = CompletableDeferred<Unit>()
    private val logger = PlayEventLogger(SystemClock::elapsedRealtime, System::currentTimeMillis)
    private var periodicSave: Job? = null
    private var writerJob: Job? = null
    private var fadingPlayer: FadingPlayer? = null
    private var isRestored = false

    override fun onCreate() {
        super.onCreate()
        val container = (application as VinylApplication).appContainer
        val player = ExoPlayer.Builder(this).build()
        player.setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true,
        )
        player.setHandleAudioBecomingNoisy(true)
        val fading = FadingPlayer(player, scope)
        fadingPlayer = fading
        val activity = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, fading).setSessionActivity(activity).setCallback(object : MediaSession.Callback {
            override fun onAddMediaItems(session: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: List<MediaItem>): ListenableFuture<List<MediaItem>> {
                return afterRestore { mediaItems }
            }
            override fun onPlaybackResumption(session: MediaSession, controller: MediaSession.ControllerInfo): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                return afterRestore {
                    val saved = container.playbackStateStore.findPlayback()
                        ?: error("No saved playback")
                    check(saved.queue.isNotEmpty()) { "No accessible songs to resume" }
                    fading.repeatMode = saved.snapshot.repeatMode
                    fading.shuffleModeEnabled = saved.snapshot.shuffleEnabled
                    MediaSession.MediaItemsWithStartPosition(saved.queue.map { it.toMediaItem() }, saved.snapshot.currentIndex, saved.snapshot.positionMs)
                }
            }
        }).build()

        // A single writer preserves event/queue ordering; Room suspends off the main thread.
        writerJob = scope.launch {
            for (checkpoint in writes) {
                try {
                    container.playbackDao.savePlayback(checkpoint.snapshot, checkpoint.queue, checkpoint.events)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { reportStorageError(error) }
            }
        }
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                logger.updatePlayback(mediaItem?.mediaId, player.isPlaying, isNewItem = true)
                saveCheckpoint(player)
            }
            override fun onEvents(changed: Player, events: Player.Events) {
                if (!isRestored) return
                if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_TIMELINE_CHANGED, Player.EVENT_REPEAT_MODE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
                    logger.updatePlayback(player.currentMediaItem?.mediaId, player.isPlaying)
                    saveCheckpoint(player)
                }
                if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                    periodicSave?.cancel()
                    if (player.isPlaying) periodicSave = scope.launch {
                        while (isActive) { delay(5_000); saveCheckpoint(player) }
                    }
                    // Focus loss/noisy pauses bypass session commands; never leave a fade running.
                    if (!player.playWhenReady) fading.cancelFade()
                }
            }
        })
        scope.launch {
            try {
                val saved = container.playbackStateStore.findPlayback()
                if (saved != null && saved.queue.isNotEmpty()) {
                    player.setMediaItems(saved.queue.map { it.toMediaItem() }, saved.snapshot.currentIndex, saved.snapshot.positionMs)
                    player.repeatMode = saved.snapshot.repeatMode
                    player.shuffleModeEnabled = saved.snapshot.shuffleEnabled
                    player.prepare() // Opening the app restores paused; only an explicit play resumes sound.
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { reportStorageError(error) }
            finally {
                isRestored = true
                restored.complete(Unit)
            }
        }
    }

    private fun saveCheckpoint(player: Player) {
        if (!isRestored) return
        logger.checkpoint()
        val queue = List(player.mediaItemCount) { index ->
            val item = player.getMediaItemAt(index)
            PlaybackQueueItem(index, item.mediaId, item.localConfiguration?.uri?.toString() ?: item.mediaId,
                item.mediaMetadata.title?.toString().orEmpty(), item.mediaMetadata.artist?.toString().orEmpty(), item.mediaMetadata.albumTitle?.toString().orEmpty())
        }
        check(writes.trySend(PlaybackCheckpoint(
            PlaybackSnapshot(currentIndex = player.currentMediaItemIndex.coerceAtLeast(0), positionMs = player.currentPosition.coerceAtLeast(0), repeatMode = player.repeatMode, shuffleEnabled = player.shuffleModeEnabled),
            queue, logger.getEvents(),
        )).isSuccess) { "Playback writer is closed" }
    }

    private fun <T> afterRestore(action: suspend () -> T): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        scope.launch {
            try { restored.await(); future.set(action()) }
            catch (cancelled: CancellationException) { future.cancel(false); throw cancelled }
            catch (error: Exception) { future.setException(error); reportStorageError(error) }
        }
        return future
    }

    private fun reportStorageError(error: Exception) {
        session?.setSessionExtras(Bundle().apply { putString("playback_storage_error", "Playback storage: ${error.localizedMessage ?: "operation failed"}") })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        periodicSave?.cancel()
        session?.let {
            fadingPlayer?.cancelFade()
            logger.updatePlayback(it.player.currentMediaItem?.mediaId, false)
            saveCheckpoint(it.player)
            it.player.release()
            it.release()
        }
        session = null
        writes.close()
        // Finish already queued transactions without blocking the main thread. Process kill uses the last checkpoint.
        serviceJob.children.filter { it != writerJob }.forEach { it.cancel() }
        writerJob?.invokeOnCompletion { serviceJob.cancel() }
        super.onDestroy()
    }
}

private data class PlaybackCheckpoint(val snapshot: PlaybackSnapshot, val queue: List<PlaybackQueueItem>, val events: List<PlayEvent>)
